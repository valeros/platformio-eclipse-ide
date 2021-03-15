/*******************************************************************************
 * Copyright (c) 2021 ArSysOp and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Nikifor Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/
package org.platformio.eclipse.ide.installer.base;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.platformio.eclipse.ide.installer.api.CommandResult;
import org.platformio.eclipse.ide.installer.api.Environment;
import org.platformio.eclipse.ide.installer.api.OS;

public final class BaseEnvironment implements Environment {

	private final OS os;
	private final Map<String, Process> running = new HashMap<>();

	public BaseEnvironment(OS os) {
		this.os = os;
	}

	private List<String> input(String command, List<String> args) {
		List<String> resolved = new LinkedList<>();
		resolved.add(command);
		resolved.addAll(args);
		return resolved;
	}

	@Override
	public CommandResult execute(String command, List<String> arguments) {
		ProcessBuilder builder = new ProcessBuilder(input(command, arguments));
		try {
			Process process = builder.start();
			int code = process.waitFor();
			return new CommandResult.Success(code);
		} catch (Exception e) {
			return new CommandResult.Failure(-1, e.getMessage());
		}
	}

	@Override
	public CommandResult execute(String command, List<String> arguments, String path) {
		ProcessBuilder builder = new ProcessBuilder(input(command, arguments));
		Map<String, String> environment = builder.environment();
		environment.put("Path", environment.get("Path") + path); //$NON-NLS-1$//$NON-NLS-2$
		environment.put("Path", environment.get("Path") + File.pathSeparator + path + "\\Scripts"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		try {
			Process process = builder.start();
			int code = process.waitFor();
			return new CommandResult.Success(code);
		} catch (Exception e) {
			e.printStackTrace();
			return new CommandResult.Failure(-1, e.getMessage());
		}
	}

	@Override
	public Path home() {
		Path directory = Optional.ofNullable(System.getenv("PLATFORMIO_HOME_DIR")).map(s -> Paths.get(s)) //$NON-NLS-1$
				.orElse(Paths.get(System.getProperty("user.home"), ".platformio")); //$NON-NLS-1$ //$NON-NLS-2$
		if (OS.Windows32.class.isInstance(os()) || OS.Windows64.class.isInstance(os())) {
			if (!isASCIIValid(directory)) {
				directory = directory.getRoot().resolve(".platformio"); //$NON-NLS-1$
			}
		}
		return directory;
	}

	private boolean isASCIIValid(Path result) {
		return result.toAbsolutePath().toString().chars().anyMatch(ch -> ch <= 127);
	}

	@Override
	public Path cache() {
		Path dir = home().resolve(".cache"); //$NON-NLS-1$
		if (!Files.isDirectory(dir)) {
			try {
				Files.createDirectories(dir);
				Files.createDirectory(dir.resolve("downloads")); //$NON-NLS-1$
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return dir;
	}

	@Override
	public Path env() {
		return home().resolve("penv"); //$NON-NLS-1$
	}

	@Override
	public OS os() {
		return os;
	}

	@Override
	public void executeLasting(String command, List<String> arguments, String path) {
		ProcessBuilder builder = new ProcessBuilder(input(command, arguments));
		Map<String, String> environment = builder.environment();
		environment.put("Path", environment.get("Path") + path); //$NON-NLS-1$//$NON-NLS-2$
		environment.put("Path", environment.get("Path") + File.pathSeparator + path + "\\Scripts"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		try {
			Process process = builder.start();
			running.put(command, process);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void killProcess(String command) {
		Process process = running.get(command);
		if (process != null) {
			process.destroy();
		}
	}

}
