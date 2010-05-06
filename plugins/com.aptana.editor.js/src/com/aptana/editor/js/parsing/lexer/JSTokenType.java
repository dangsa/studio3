package com.aptana.editor.js.parsing.lexer;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.aptana.parsing.lexer.ITypePredicate;

public enum JSTokenType implements ITypePredicate
{
	UNDEFINED("undefined.html"),
	KEYWORD("keyword.operator.js"),
	SUPPORT_FUNCTION("support.function.js"),
	EVENT_HANDLER_FUNCTION("support.function.event-handler.js"),
	DOM_FUNCTION("support.function.dom.js"),
	FIREBUG_FUNCTION("support.function.js.firebug"),
	OPERATOR("keyword.operator.js"),
	SUPPORT_CONSTANT("support.constant.js"),
	DOM_CONSTANTS("support.constant.dom.js"),
	SOURCE("source.js"),
	CONTROL_KEYWORD("keyword.control.js"),
	STORAGE_TYPE("storage.type.js"),
	STORAGE_MODIFIER("storage.modifier.js"),
	SUPPORT_CLASS("support.class.js"),
	SUPPORT_DOM_CONSTANT("support.constant.dom.js"),
	TRUE("constant.language.boolean.true.js"),
	FALSE("constant.language.boolean.false.js"),
	NULL("constant.language.null.js"),
	CONSTANT("constant.language.js"),
	VARIABLE("variable.language.js"),
	OTHER_KEYWORD("keyword.other.js"),
	SEMICOLON("punctuation.terminator.statement.js"),
	PARENTHESIS("meta.brace.round.js"),
	BRACKET("meta.brace.square.js"),
	CURLY_BRACE("meta.brace.curly.js"),
	COMMA("meta.delimiter.object.comma.js"),
	NUMBER("constant.numeric.js");

	private static final Map<String, JSTokenType> NAME_MAP;
	private String _scope;

	/**
	 * static
	 */
	static
	{
		NAME_MAP = new HashMap<String, JSTokenType>();

		for (JSTokenType type : EnumSet.allOf(JSTokenType.class))
		{
			NAME_MAP.put(type.getScope(), type);
		}
	}

	/**
	 * get
	 * 
	 * @param scope
	 * @return
	 */
	public static final JSTokenType get(String scope)
	{
		return (NAME_MAP.containsKey(scope)) ? NAME_MAP.get(scope) : UNDEFINED;
	}

	/**
	 * CSSTokenTypes
	 * 
	 * @param scope
	 */
	private JSTokenType(String scope)
	{
		this._scope = scope;
	}

	/**
	 * getName
	 * 
	 * @return
	 */
	public String getScope()
	{
		return this._scope;
	}

	/*
	 * (non-Javadoc)
	 * @see com.aptana.editor.css.parsing.lexer.ITypePredicate#isDefined()
	 */
	@Override
	public boolean isDefined()
	{
		return (this != UNDEFINED);
	}

	/**
	 * toString
	 */
	public String toString()
	{
		return this.getScope();
	}
}
