package com.sqleo.environment.ctrl.commands;

import java.util.List;
import java.util.regex.Pattern;

import com.sqleo.environment.Application;

public class QuitCommand extends AbstractCommand {

	private static final String USAGE = "Usage: quit, Description: Quits the command mode";
	public static String NAME = "quit";

	@Override
	public String getCommand() {
		return NAME;
	}

	@Override
	public String getCommandUsage() {
		return USAGE;
	}

	@Override
	public List<String> getCommandOptions() {
		// no options
		return null;
	}

	@Override
	public int getCommandTokensLength() {
		return 1;
	}
	
	@Override
	protected Pattern getCommandRegex(){
		return Pattern.compile("(^quit)");
	}

	@Override
	public CommandExecutionResult execute(String command) {
		final List<String> tokens = tokenizeCommand(command);
		final CommandExecutionResult result = new CommandExecutionResult();
		if (tokens.isEmpty()) {
			result.setCode(CommandExecutionResult.INVALID);
			return result;
		}
		final String quit = tokens.get(0);
		if (!quit.equals(NAME)) {
			Application.alert("Given option: " + quit + " is invalid, expected:" + NAME + " see usage" + USAGE);
			result.setCode(CommandExecutionResult.INVALID);
			return result;
		}
		result.setDetail("Bye! quitting command mode, all your previous command settings will not be active anymore, execute any other command to activate your settings");
		result.setCode(CommandExecutionResult.SUCCESS);
		return result;
	}

}
