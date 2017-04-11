import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.function.Consumer;

import Parser.CommandType;

public class Parser implements AutoCloseable
{
	enum CommandType
	{
		NONE,
		ARITHMETIC,
		PUSH,
		POP,
		LABEL,
		GOTO,
		IF,
		FUNCTION,
		RETURN,
		CALL,
	}

	private Scanner		inputFile;
	private int			lineNumber;
	private String		rawLine;

	private String[]	splitLine;
	private CommandType	commandType;
	private String		arg1;
	private int			arg2;

	/**
	 * Parses a hack asm file.
	 * 
	 * @param inputFilePath
	 *            - path to the hack asm file.
	 */
	public Parser(String inputFilePath)
	{
		lineNumber = 0;

		try
		{
			inputFile = new Scanner(Paths.get(inputFilePath));
		}
		catch (IOException e)
		{
			System.err.println("Could not open file \"" + inputFilePath + "\".");
			System.exit(2); // 2 = could not open file
		}
	}

	/**
	 * Tests to see if you can call {@link #advance()}. Will close the stream if
	 * there are no more lines to parse
	 * 
	 * @return True if there are more commands to process in the file
	 */
	public boolean hasMoreCommands()
	{
		if (!inputFile.hasNextLine())
		{
			// should probably provide a close method
			inputFile.close();
			return false;
		}
		else
		{
			return true;
		}
	}

	/**
	 * Advanced the parser, parsing the line, thus mutating most of it's fields
	 * such as commandType (to represent the command type of the parsed line) to
	 * the destMnemonic (if it had one).
	 */
	public void advance()
	{
		lineNumber++;
		rawLine = inputFile.nextLine();
		cleanAndSplitLine();

		clearParsedValues();
		parse();
	}

	/**
	 * Clears the parsed values for the next run of parsing.
	 */
	private void clearParsedValues()
	{
		// command type is always set so no need to clear it
		arg1 = null;
		arg2 = -1;
	}

	/**
	 * Parses the line based on the command type.
	 */
	private void parse()
	{
		parseCommandType();
		switch (commandType)
		{
			case A:
			case LABEL:
				parseSymbol();
				break;
			case C:
				parseDest();
				parseComp();
				parseJump();
				break;
			default:
				break;
		}
	}

	/**
	 * Determine the command type of the line.
	 */
	private void parseCommandType()
	{
		if (splitLine == null || splitLine[0].isEmpty())
		{
			commandType = CommandType.NONE;
			return;
		}

		switch (splitLine[0])
		{
			case "add":
			case "sub":
			case "neg":
			case "eq":
			case "gt":
			case "lt":
			case "and":
			case "or":
			case "not":
				commandType = CommandType.ARITHMETIC;
				return;
			case "push":
				commandType = CommandType.PUSH;
				return;
			case "pop":
				commandType = CommandType.POP;
				return;
			//vm part 2 stuff here
				return;
		}
	}

	/**
	 * Removes all whitespace and everything past a comment leaving only the
	 * command to be parsed.
	 */
	private void cleanAndSplitLine()
	{
		// pardon the small regex :D
		// I just don't want to spam lots of replaceAll s
		//TODO check this code (maybe 58fd9bd is a better version

		int commentIndex = rawLine.indexOf("//");
		if (commentIndex >= 0)
		{
			splitLine = rawLine.substring(0, commentIndex).toLowerCase().split(" ");
		}
		else
		{
			splitLine = rawLine.toLowerCase().split(" ");
		}
	}

	public void close()
	{
		inputFile.close();
	}
}
