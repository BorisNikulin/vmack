import java.util.HashMap;
import java.util.function.Consumer;

import Parser.CommandType;
import sun.security.provider.JavaKeyStore.CaseExactJKS;

/**
 * Writes each line of a vm to hack translation to a supplied producer.
 * 
 * @author Boris
 *
 */
public class CodeWriter
{
	// TODO add a verbosity level to suppress comments or any extra characters
	private Consumer<String> out;
	private int labelCounter;

	private enum ARITHMETIC_TYPE
	{
		UNARY,
		BINARY,
		LOGICAL_EXPRESSION,
	}

	private final static HashMap<String, Character>	OPERATOR_LOOKUP				= new HashMap<>(6);
	private final static HashMap<String, String>	LOGICAL_EXPRESSION_LOOKUP	= new HashMap<>(3);

	static
	{
		OPERATOR_LOOKUP.put("add", '+');
		OPERATOR_LOOKUP.put("sub", '-');
		OPERATOR_LOOKUP.put("neg", '-');
		OPERATOR_LOOKUP.put("and", '&');
		OPERATOR_LOOKUP.put("or", '|');
		OPERATOR_LOOKUP.put("not", '!');

		LOGICAL_EXPRESSION_LOOKUP.put("eq", "JEQ");
		LOGICAL_EXPRESSION_LOOKUP.put("gt", "JGT");
		LOGICAL_EXPRESSION_LOOKUP.put("lt", "JLT");
	}

	public CodeWriter(Consumer<String> out)
	{
		this.out = out;
		labelCounter = 0;
	}

	public void writeArithemtic(String command)
	{
		switch (parseArithmeticType(command))
		{
			case UNARY:
				writeArithUnary(OPERATOR_LOOKUP.get(command));
				break;
			case BINARY:
				writeArithBinary(OPERATOR_LOOKUP.get(command));
				break;
			case LOGICAL_EXPRESSION:
				writeArithLogicalExpression(LOGICAL_EXPRESSION_LOOKUP.get(command));
				break;
		}
	}

	private void writeArithUnary(char op)
	{
		out.accept("//arith unary with op: " + op);
		out.accept("\t@SP");
		out.accept("\tAM=M-1");
		out.accept("\tM=" + op + "M");
		out.accept("");
	}

	private void writeArithBinary(char op)
	{
		out.accept("//arith binary with op: " + op);
		out.accept("\t@SP");
		out.accept("\tAM=M-1");
		out.accept("\tD=M");
		out.accept("\tA=A-1");
		out.accept("\tM=M" + op + "D");
		out.accept("");
	}

	private void writeArithLogicalExpression(String op)
	{
		out.accept("//arith logical expression with op: " + op);
		out.accept("\t@SP");
		out.accept("\tA=M-1");
		out.accept("\tA=A-1");
		out.accept("\tD=M");
		out.accept("\tA=A+1");
		out.accept("\tD=D-M");
		out.accept("\t@" + labelCounter + "T");
		out.accept("\tD;" + op);
		out.accept("\t@" + labelCounter + "F");
		out.accept("\tD=0");
		out.accept("\t0;JMP");
		out.accept("(" + labelCounter + "T)");
		out.accept("\tD=-1");
		out.accept("(" + labelCounter + "F)");
		out.accept("\t@SP");
		out.accept("\tAM=M-1");
		out.accept("\tA=A-1");
		out.accept("\tM=D");
		out.accept("");
		
		labelCounter++;
	}

	private ARITHMETIC_TYPE parseArithmeticType(String command)
	{
		if (OPERATOR_LOOKUP.containsKey(command))
		{
			return ARITHMETIC_TYPE.LOGICAL_EXPRESSION;
		}
		else
		{
			switch (command)
			{
				case "neg":
				case "not":
					return ARITHMETIC_TYPE.UNARY;
				default:
					return ARITHMETIC_TYPE.BINARY;
			}
		}
	}

	public void writePushPop(CommandType commandType, String segment, int index)
	{

	}

	/**
	 * Set's the consumer that will consume each line of output from the code
	 * writer.
	 * 
	 * @param out
	 *            the consumer of this classes output
	 */
	public void setOut(Consumer<String> out)
	{
		this.out = out;
	}

}
