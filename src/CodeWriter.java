import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Writes each line of a vm to hack translation to a supplied producer.
 * 
 * @author Boris
 *
 */
public class CodeWriter
{
	private enum ARITHMETIC_TYPE
	{
		UNARY,
		BINARY,
		LOGICAL_EXPRESSION,
	}

	// TODO add a verbosity level to suppress comments or any extra characters
	private Consumer<String>						out;
	private int										labelCounter;

	private String									staticQualifier;

	private final static HashMap<String, Character>	OPERATOR_LOOKUP				= new HashMap<>(6);
	private final static HashMap<String, String>	LOGICAL_EXPRESSION_LOOKUP	= new HashMap<>(3);
	private final static HashMap<String, String>	SEGMENT_LOOKUP				= new HashMap<>(4);

	static
	{
		OPERATOR_LOOKUP.put("add", '+');
		OPERATOR_LOOKUP.put("sub", '-');
		OPERATOR_LOOKUP.put("neg", '-');
		OPERATOR_LOOKUP.put("and", '&');
		OPERATOR_LOOKUP.put("or", '|');
		OPERATOR_LOOKUP.put("not", '!');

		// TODO look into combining look up tables (or not)
		LOGICAL_EXPRESSION_LOOKUP.put("eq", "JEQ");
		LOGICAL_EXPRESSION_LOOKUP.put("gt", "JGT");
		LOGICAL_EXPRESSION_LOOKUP.put("lt", "JLT");

		SEGMENT_LOOKUP.put("argument", "ARG");
		SEGMENT_LOOKUP.put("local", "LCL");
		SEGMENT_LOOKUP.put("this", "THIS");
		SEGMENT_LOOKUP.put("that", "THAT");
	}

	public CodeWriter(Consumer<String> out, String staticQualifier)
	{
		this.out = out;
		labelCounter = 0;

		this.staticQualifier = staticQualifier;
	}

	/**
	 * Writes each line of translation to the consumer according to the vm
	 * specification.
	 * 
	 * @param command
	 *            - the arithmetic command to translate.
	 */
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
		out.accept("  //arith unary with op: " + op);
		out.accept("  @SP");
		out.accept("  A=M-1");
		out.accept("  M=" + op + "M");
		out.accept("");
	}

	private void writeArithBinary(char op)
	{
		out.accept("  //arith binary with op: " + op);
		out.accept("  @SP");
		out.accept("  AM=M-1");
		out.accept("  D=M");
		out.accept("  A=A-1");
		out.accept("  M=M" + op + "D");
		out.accept("");
	}

	private void writeArithLogicalExpression(String op)
	{
		out.accept("  //arith logical expression with op: " + op);
		out.accept("  @SP");
		out.accept("  A=M-1");
		out.accept("  A=A-1");
		out.accept("  D=M");
		out.accept("  A=A+1");
		out.accept("  D=D-M");
		out.accept("  @" + labelCounter + "T");
		out.accept("  D;" + op);
		out.accept("  @" + labelCounter + "F");
		out.accept("  D=0");
		out.accept("  0;JMP");
		out.accept("(" + labelCounter + "T)");
		out.accept("  D=-1");
		out.accept("(" + labelCounter + "F)");
		out.accept("  @SP");
		out.accept("  AM=M-1");
		out.accept("  A=A-1");
		out.accept("  M=D");
		out.accept("");

		labelCounter++;
	}

	private ARITHMETIC_TYPE parseArithmeticType(String command)
	{
		if (!OPERATOR_LOOKUP.containsKey(command))
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

	/**
	 * Writes each line of translation to the consumer according to the vm
	 * specification.
	 * 
	 * @param commandType
	 *            - Whether to push or pop.
	 * @param segment
	 *            - virtual memory segment.
	 * @param index
	 *            - index within the memory segment.
	 */
	public void writePushPop(Parser.CommandType commandType, String segment, int index)
	{
		switch (commandType)
		{
			case PUSH:
				writePush(segment, index);
				break;
			case POP:
				writePop(segment, index);
				break;
			default:
				throw new IllegalArgumentException("The only acceptable Parser.CommandType enums are PUSH and POP");
		}
	}

	private void writePush(String segment, int index)
	{
		switch (segment)
		{
			case "constant":
				writePushConstant(index);
				break;
			case "local":
			case "argument":
			case "this":
			case "that":
				writePushSimpleSegment(SEGMENT_LOOKUP.get(segment), index);
				break;
			// TODO bound checking on temp?
			case "temp":
				writePushTemp(index);
				break;
			case "pointer":
				writePushPointer(index);
				break;
			case "static":
				writePushStatic(index);
				break;
			default:
				throw new IllegalArgumentException("Invalid segment: " + segment);
		}
	}

	private void writePushConstant(int index)
	{
		out.accept("  //push constant " + index);
		out.accept("  @" + index);
		out.accept("  D=A");
		out.accept("");

		writePushD();
	}

	private void writePushSimpleSegment(String segment, int index)
	{
		out.accept("  //push simple segment " + segment + "[" + index + "]");
		out.accept("  @" + index);
		out.accept("  D=A");
		out.accept("  @" + segment);
		out.accept("  A=M+D");
		out.accept("  D=M");
		out.accept("");

		writePushD();
	}

	private void writePushTemp(int index)
	{
		out.accept("  //push temp[" + index + "]");
		out.accept("  @" + index);
		out.accept("  D=A");
		out.accept("  @R5");
		out.accept("  A=A+D");
		out.accept("  D=M");
		out.accept("");

		writePushD();
	}

	private void writePushPointer(int index)
	{
		// TODO simplify if s (DRY)
		if (index == 0)
		{
			out.accept("  //push pointer " + index);
			out.accept("  @THIS");
			out.accept("  D=M");
			out.accept("");

			writePushD();
		}
		else if (index == 1)
		{
			out.accept("  //push pointer " + index);
			out.accept("  @THAT");
			out.accept("  D=M");
			out.accept("");

			writePushD();
		}
		else
		{
			throw new IllegalArgumentException("Invalid offset to pointer segment: " + index);
		}
	}

	private void writePushStatic(int index)
	{
		out.accept("  //push static[" + index + "]");
		out.accept("  @" + staticQualifier + "." + index);
		out.accept("  D=M");
		out.accept("");

		writePushD();
	}

	private void writePop(String segment, int index)
	{
		switch (segment)
		{
			case "local":
			case "argument":
			case "this":
			case "that":
				writePopSimpleSegment(SEGMENT_LOOKUP.get(segment), index);
				break;
			// TODO bound checking on temp?
			case "temp":
				writePopTemp(index);
				break;
			case "pointer":
				writePopPointer(index);
				break;
			case "static":
				writePopStatic(index);
				break;
			default:
				throw new IllegalArgumentException("Invalid segment: " + segment);
		}
	}

	private void writePopSimpleSegment(String segment, int index)
	{
		out.accept("  //pop simple segment " + segment + "[" + index + "]");
		out.accept("  @" + index);
		out.accept("  D=A");
		out.accept("  @" + segment);
		out.accept("  D=M+D");
		out.accept("  @R13");
		out.accept("  M=D");
		out.accept("");

		writePopD();

		out.accept("  @R13");
		out.accept("  A=M");
		out.accept("  M=D");
		out.accept("");
	}

	private void writePopTemp(int index)
	{
		out.accept("  //pop temp[" + index + "]");
		out.accept("  @" + index);
		out.accept("  D=A");
		out.accept("  @R5");
		out.accept("  D=A+D");
		out.accept("  @R13");
		out.accept("  M=D");
		out.accept("");

		writePopD();

		out.accept("  @R13");
		out.accept("  A=M");
		out.accept("  M=D");
		out.accept("");
	}

	private void writePopPointer(int index)
	{
		// TODO simplify if s (DRY)
		writePopD();

		if (index == 0)
		{
			out.accept("  //pop pointer " + index);
			out.accept("  @THIS");
			out.accept("  M=D");
			out.accept("");
		}
		else if (index == 1)
		{
			out.accept("  //pop pointer " + index);
			out.accept("  @THAT");
			out.accept("  M=D");
			out.accept("");
		}
		else
		{
			throw new IllegalArgumentException("Invalid offset to pointer segment: " + index);
		}
	}

	private void writePopStatic(int index)
	{
		writePopD();

		out.accept("  //pop static[" + index + "]");
		out.accept("  @" + staticQualifier + "." + index);
		out.accept("  M=D");
		out.accept("");

	}

	private void writePushD()
	{
		out.accept("  //push D");
		out.accept("  @SP");
		out.accept("  AM=M+1");
		out.accept("  A=A-1");
		out.accept("  M=D");
		out.accept("");
	}

	private void writePopD()
	{
		out.accept("  //pop D");
		out.accept("  @SP");
		out.accept("  AM=M-1");
		out.accept("  D=M");
		out.accept("");
	}

	/**
	 * @return the out
	 */
	public Consumer<String> getOut()
	{
		return out;
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

	/**
	 * @return the staticQualifier
	 */
	public String getStaticQualifier()
	{
		return staticQualifier;
	}

	/**
	 * @param staticQualifier
	 *            the staticQualifier to set
	 */
	public void setStaticQualifier(String staticQualifier)
	{
		this.staticQualifier = staticQualifier;
	}
}
