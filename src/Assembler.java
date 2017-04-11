
//Author info here
//TODO: don't forget to document each method in all classes!
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class Assembler
{

	// ALGORITHM:
	// get input file name
	// create output file name and stream

	// create symbol table
	// do first pass to build symbol table (no output yet!)
	// do second pass to output translated ASM to HACK code

	// print out "done" message to user
	// close output file stream
	public static void main(String[] args)
	{
		String inputFileName, outputFileName;
		PrintWriter outputFile = null; // keep compiler happy
		// TODO remove following line?
		// int romAddress, ramAddress;

		// get input file name from command line or console input
		if (args.length == 1)
		{
			System.out.println("command line arg = " + args[0]);
			inputFileName = args[0];
		}
		else
		{
			Scanner keyboard = new Scanner(System.in);

			System.out.println("Please enter vm file name you would like to translate.");
			System.out.println("Don't forget the .vm extension: ");
			inputFileName = keyboard.nextLine();

			keyboard.close();
		}

		// TODO check if inputFile even exists

		String[] pathSections = dissectPath(inputFileName);

		outputFileName = pathSections[0] + pathSections[1] + ".asm";

		try
		{
			outputFile = new PrintWriter(outputFileName);
		}
		catch (FileNotFoundException ex)
		{
			System.err.println("Could not open output file " + outputFileName);
			System.err.println("Run program again, make sure you have write permissions, etc.");
			System.exit(0);
		}

		firstPass(inputFileName, pathSections[1], outputFile);

		outputFile.close();
	}

	private static void firstPass(String inputFileName, String fileNameForStaticQualifier, PrintWriter outputFile)
	{
		Parser parser = new Parser(inputFileName);
		CodeWriter writer = new CodeWriter(outputFile::println, fileNameForStaticQualifier);
		// CodeWriter writer = new CodeWriter(System.out::println, "stdOut");

		while (parser.hasMoreCommands())
		{
			parser.advance();

			switch (parser.getCommandType())
			{
				case ARITHMETIC:
					writer.writeArithemtic(parser.getArg1());
					break;
				case PUSH:
				case POP:
					writer.writePushPop(parser.getCommandType(), parser.getArg1(), parser.getArg2());
					break;
				default:
					break;
			}

		}

		parser.close();
	}

	/**
	 * Takes a file path as a string and returns an array with the first element
	 * being the part of the string before the name, the second element with the
	 * name, and the third part of the array with the extension. Where there is
	 * no applicable part from the file path, the corresponding section will be
	 * empty. For instance if there is no extension the the third element of the
	 * returned array will have an empty string. Concatenating the array
	 * together from the first to the last element will result in the original
	 * file path.
	 * 
	 * @param filePath
	 * @return
	 */
	public static String[] dissectPath(String filePath)
	{
		int nameStartIndex;
		if ((nameStartIndex = filePath.lastIndexOf('/')) >= 0)
		{
			nameStartIndex += 1;
		}
		else if ((nameStartIndex = filePath.lastIndexOf('\\')) >= 0)
		{
			nameStartIndex += 1;
		}
		else
		{
			nameStartIndex = 0;
		}

		int nameEndIndex = filePath.indexOf('.');
		if (nameEndIndex < 0)
		{
			nameEndIndex = filePath.length();
		}

		String[] dissectedPaths = new String[3];

		dissectedPaths[0] = filePath.substring(0, nameStartIndex);
		dissectedPaths[1] = filePath.substring(nameStartIndex, nameEndIndex);
		dissectedPaths[2] = filePath.substring(nameEndIndex, filePath.length());

		return dissectedPaths;
	}

}