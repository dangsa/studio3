package com.aptana.git.ui.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.ui.ISharedImages;
import org.eclipse.team.ui.TeamImages;
import org.eclipse.ui.PlatformUI;

import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.editor.common.theme.IThemeManager;
import com.aptana.git.core.GitPlugin;
import com.aptana.git.core.model.BranchAddedEvent;
import com.aptana.git.core.model.BranchChangedEvent;
import com.aptana.git.core.model.BranchRemovedEvent;
import com.aptana.git.core.model.ChangedFile;
import com.aptana.git.core.model.GitRepository;
import com.aptana.git.core.model.IGitRepositoriesListener;
import com.aptana.git.core.model.IGitRepositoryListener;
import com.aptana.git.core.model.IGitRepositoryManager;
import com.aptana.git.core.model.IndexChangedEvent;
import com.aptana.git.core.model.PullEvent;
import com.aptana.git.core.model.PushEvent;
import com.aptana.git.core.model.RepositoryAddedEvent;
import com.aptana.git.core.model.RepositoryRemovedEvent;
import com.aptana.git.ui.GitUIPlugin;

public class GitLightweightDecorator extends BaseLabelProvider implements ILightweightLabelDecorator,
		IGitRepositoryListener, IGitRepositoriesListener
{

	private static final String DIRTY_PREFIX = "* "; //$NON-NLS-1$
	private static final String DECORATOR_ID = "com.aptana.git.ui.internal.GitLightweightDecorator"; //$NON-NLS-1$

	/**
	 * Define a cached image descriptor which only creates the image data once
	 */
	private static class CachedImageDescriptor extends ImageDescriptor
	{
		ImageDescriptor descriptor;

		ImageData data;

		public CachedImageDescriptor(ImageDescriptor descriptor)
		{
			this.descriptor = descriptor;
		}

		public ImageData getImageData()
		{
			if (data == null)
			{
				data = descriptor.getImageData();
			}
			return data;
		}
	}

	private static ImageDescriptor conflictImage;
	private static ImageDescriptor untrackedImage;
	private static ImageDescriptor stagedAddedImage;
	private static ImageDescriptor stagedRemovedImage;

	static
	{
		conflictImage = new CachedImageDescriptor(TeamImages.getImageDescriptor(ISharedImages.IMG_CONFLICT_OVR));
		untrackedImage = new CachedImageDescriptor(ImageDescriptor.createFromURL(GitUIPlugin.getDefault().getBundle()
				.getEntry("icons/ovr/untracked.gif"))); //$NON-NLS-1$
		stagedAddedImage = new CachedImageDescriptor(ImageDescriptor.createFromURL(GitUIPlugin.getDefault().getBundle()
				.getEntry("icons/ovr/staged_added.gif"))); //$NON-NLS-1$
		stagedRemovedImage = new CachedImageDescriptor(ImageDescriptor.createFromURL(GitUIPlugin.getDefault()
				.getBundle().getEntry("icons/ovr/staged_removed.gif"))); //$NON-NLS-1$
	}

	private IPreferenceChangeListener fThemeChangeListener;

	public GitLightweightDecorator()
	{
		getGitRepositoryManager().addListener(this);
		getGitRepositoryManager().addListenerToEachRepository(this);
		fThemeChangeListener = new IPreferenceChangeListener()
		{

			@Override
			public void preferenceChange(PreferenceChangeEvent event)
			{
				if (event.getKey().equals(IThemeManager.THEME_CHANGED))
				{
					refresh();
				}
			}
		};
		new InstanceScope().getNode(CommonEditorPlugin.PLUGIN_ID).addPreferenceChangeListener(fThemeChangeListener);
	}

	protected IGitRepositoryManager getGitRepositoryManager()
	{
		return GitPlugin.getDefault().getGitRepositoryManager();
	}

	public void decorate(Object element, IDecoration decoration)
	{
		final IResource resource = getResource(element);
		if (resource == null)
			return;

		// Don't decorate if the workbench is not running
		if (!PlatformUI.isWorkbenchRunning())
			return;

		// Don't decorate if UI plugin is not running
		GitUIPlugin activator = GitUIPlugin.getDefault();
		if (activator == null)
			return;

		// Don't decorate the workspace root
		if (resource.getType() == IResource.ROOT)
			return;

		// Don't decorate non-existing resources
		if (!resource.exists() && !resource.isPhantom())
			return;

		switch (resource.getType())
		{
			case IResource.PROJECT:
				decorateProject(decoration, resource);
				// fall through intentionally!
			case IResource.FOLDER:
				decorateFolder(decoration, resource);
				break;
			case IResource.FILE:
				decorateFile(decoration, resource);
				break;
		}
	}

	private void decorateFolder(IDecoration decoration, IResource resource)
	{
		GitRepository repo = getRepo(resource);
		if (repo == null)
			return;

		if (repo.resourceOrChildHasChanges(resource))
		{
			decoration.addPrefix(DIRTY_PREFIX);
		}
	}

	private void decorateFile(IDecoration decoration, final IResource resource)
	{
		IFile file = (IFile) resource;
		GitRepository repo = getRepo(resource);
		if (repo == null)
			return;

		ChangedFile changed = repo.getChangedFileForResource(file);
		if (changed == null)
		{
			return;
		}

		ImageDescriptor overlay = null;
		// Unstaged trumps staged when decorating. One file may have both staged and unstaged changes.
		if (changed.hasUnstagedChanges())
		{
			decoration.setForegroundColor(GitColors.redFG());
			decoration.setBackgroundColor(GitColors.redBG());
			if (changed.getStatus() == ChangedFile.Status.NEW)
			{
				overlay = untrackedImage;
			}
			else if (changed.getStatus() == ChangedFile.Status.UNMERGED)
			{
				overlay = conflictImage;
			}
		}
		else if (changed.hasStagedChanges())
		{
			decoration.setForegroundColor(GitColors.greenFG());
			decoration.setBackgroundColor(GitColors.greenBG());
			if (changed.getStatus() == ChangedFile.Status.DELETED)
			{
				overlay = stagedRemovedImage;
			}
			else if (changed.getStatus() == ChangedFile.Status.NEW)
			{
				overlay = stagedAddedImage;
			}
		}
		decoration.addPrefix(DIRTY_PREFIX);
		if (overlay != null)
			decoration.addOverlay(overlay);
	}

	private void decorateProject(IDecoration decoration, final IResource resource)
	{
		GitRepository repo = getRepo(resource);
		if (repo == null)
			return;

		StringBuilder builder = new StringBuilder();
		builder.append(" ["); //$NON-NLS-1$
		String branch = repo.currentBranch();
		builder.append(branch);
		String[] commits = repo.commitsAhead(branch);
		if (commits != null && commits.length > 0)
		{
			builder.append("+").append(commits.length); //$NON-NLS-1$
		}
		else
		{
			// Happens way less frequently. usually only if you've fetched but haven't merged (which usually happens
			// when you pull on one branch and then switch back to another that had changes not yet merged in yet)
			commits = repo.commitsBehind(branch);
			if (commits != null && commits.length > 0)
				builder.append("-").append(commits.length); //$NON-NLS-1$
		}
		builder.append("]"); //$NON-NLS-1$
		decoration.addSuffix(builder.toString());
	}

	

	@Override
	public void dispose()
	{
		try
		{
			getGitRepositoryManager().removeListener(this);
			getGitRepositoryManager().removeListenerFromEachRepository(this);
			new InstanceScope().getNode(CommonEditorPlugin.PLUGIN_ID).removePreferenceChangeListener(
					fThemeChangeListener);
		}
		finally
		{
			super.dispose();
		}
	}

	private static IResource getResource(Object element)
	{

		IResource resource = null;
		if (element instanceof IResource)
		{
			resource = (IResource) element;
		}
		else if (element instanceof IAdaptable)
		{
			final IAdaptable adaptable = (IAdaptable) element;
			resource = (IResource) adaptable.getAdapter(IResource.class);
		}
		return resource;
	}

	protected GitRepository getRepo(IResource resource)
	{
		if (resource == null)
			return null;
		IProject project = resource.getProject();
		if (project == null)
			return null;
		return getGitRepositoryManager().getAttached(project);
	}

	/**
	 * Post the label event to the UI thread
	 * 
	 * @param event
	 *            The event to post
	 */
	private void postLabelEvent(final LabelProviderChangedEvent event)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				fireLabelProviderChanged(event);
			}
		});
	}

	public void indexChanged(IndexChangedEvent e)
	{
		// FIXME Force a total refresh if the number of changed files is over some maximum!
		Set<IResource> resources = addChangedFiles(e.getRepository(), e.changedFiles());
		// Need to mark all parents up to project for refresh so the dirty flag can get recomputed for these
		// ancestor folders!
		resources.addAll(getAllAncestors(resources));
		// FIXME Only add projects if this was a commit (so the plus/minus changes), not just a file edited/staged/unstaged
		// Also refresh any project sharing this repo (so the +/- commits ahead can be refreshed)
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects())
		{
			GitRepository repo = getGitRepositoryManager().getAttached(project);
			if (repo != null && repo.equals(e.getRepository()))
				resources.add(project);
		}
		postLabelEvent(new LabelProviderChangedEvent(this, resources.toArray()));
	}

	private Collection<? extends IResource> getAllAncestors(Set<IResource> resources)
	{
		Collection<IResource> ancestors = new HashSet<IResource>();
		for (IResource resource : resources)
		{
			IResource child = resource;
			IContainer parent = null;
			while ((parent = child.getParent()) != null)
			{
				if (parent.getType() == IResource.PROJECT || parent.getType() == IResource.ROOT)
					break;
				ancestors.add(parent);
				child = parent;
			}
		}
		return ancestors;
	}

	private Set<IResource> addChangedFiles(GitRepository repository, Collection<ChangedFile> changedFiles)
	{
		String workingDir = repository.workingDirectory();
		Set<IResource> resources = new HashSet<IResource>();
		for (ChangedFile changedFile : changedFiles)
		{
			IResource resource = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(
					new Path(workingDir).append(changedFile.getPath()));
			if (resource != null)
				resources.add(resource);
		}
		return resources;
	}

	public void repositoryAdded(RepositoryAddedEvent e)
	{
		e.getRepository().addListener(this);
		Set<IResource> resources = addChangedFiles(e.getRepository(), e.getRepository().index().changedFiles());
		resources.add(e.getProject());
		postLabelEvent(new LabelProviderChangedEvent(this, resources.toArray()));
	}

	public void repositoryRemoved(RepositoryRemovedEvent e)
	{
		e.getRepository().removeListener(this);
		Set<IResource> resources = addChangedFiles(e.getRepository(), e.getRepository().index().changedFiles());
		resources.add(e.getProject());
		postLabelEvent(new LabelProviderChangedEvent(this, resources.toArray()));
	}

	/**
	 * Perform a blanket refresh of all decorations this is very bad performance wise. Need to avoid using this and
	 * always just use deltas if possible!
	 */
	private static void refresh()
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				GitUIPlugin.getDefault().getWorkbench().getDecoratorManager().update(DECORATOR_ID);
			}
		});
	}

	public void branchChanged(BranchChangedEvent e)
	{
		Set<IResource> resources = new HashSet<IResource>();
		GitRepository repo = e.getRepository();
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects)
		{
			if (repo.equals(getGitRepositoryManager().getAttached(project)))
				resources.add(project);
		}
		// Project labels need to change, but the dirty/stage/unstaged flags should stay same (can't change branch with staged/unstaged changes, dirty carry over).
		postLabelEvent(new LabelProviderChangedEvent(this, resources.toArray()));
	}

	@Override
	public void pulled(PullEvent e)
	{
		// FIXME do nothing? Call refresh?
	}

	@Override
	public void pushed(PushEvent e)
	{
		// FIXME do nothing? Call refresh?
	}

	@Override
	public void branchAdded(BranchAddedEvent e)
	{
		// do nothing
	}

	@Override
	public void branchRemoved(BranchRemovedEvent e)
	{
		// do nothing
	}
}
