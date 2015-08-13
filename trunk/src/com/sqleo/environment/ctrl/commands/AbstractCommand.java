package com.sqleo.environment.ctrl.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sqleo.common.util.Text;
import com.sqleo.environment.Application;

public abstract class AbstractCommand implements Command {

	protected  abstract Pattern getCommandRegex();
	
	protected List<String> tokenizeCommand(final String command) {
		if(getCommandRegex()!=null){
			return tokenizeCommandWithRegex(command);
		}else{
			return tokenizeCommandWithTokenizer(command);
		}
	}
	
	protected List<String> tokenizeCommandWithTokenizer(final String command) {
		final StringTokenizer tokens = new StringTokenizer(command);
		final List<String> commandWithOptions = new ArrayList<String>();
		if (tokens.countTokens() < getCommandTokensLength()) {
			Application
					.alert("Invalid arguments received for command :" + command + ", see usage:" + getCommandUsage());
			return commandWithOptions;
		}
		while (tokens.hasMoreTokens()) {
			commandWithOptions.add(tokens.nextToken());
		}
		assert !commandWithOptions.isEmpty();
		assert getCommand().startsWith(commandWithOptions.get(0));
		return commandWithOptions;
	}
	
	protected List<String> tokenizeCommandWithRegex(final String command) {
		final Matcher matcher = getCommandRegex().matcher(command);
		final List<String> commandWithOptions = new ArrayList<String>();
		if (matcher.groupCount() < getCommandTokensLength()) {
			Application
					.alert("Invalid arguments received for command :" + command + ", see usage:" + getCommandUsage());
			return commandWithOptions;
		}
		while (matcher.find()) {
			for(int i = 1 ; i <= matcher.groupCount() ; i++) {
				commandWithOptions.add(Text.trimBoth(matcher.group(i)));
			}
		}
		assert !commandWithOptions.isEmpty();
		assert getCommand().startsWith(commandWithOptions.get(0));
		return commandWithOptions;
	}
}