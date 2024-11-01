package de.ddm.configuration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import de.ddm.singletons.InputConfigurationSingleton;
import de.ddm.singletons.SystemConfigurationSingleton;

public abstract class Command {

	abstract int getDefaultPort();

	@Parameter(names = {"-h", "--host"}, description = "This machine's host name that we use to bind this application against", required = false)
	String host = SystemConfigurationSingleton.get().getHost();

	@Parameter(names = {"-ia", "--ipAddress"}, description = "This machine's IP that we use to bind this application against", required = false)
	String ipAddress = SystemConfigurationSingleton.get().getIpAddress();

	@Parameter(names = {"-p", "--port"}, description = "This machines port that we use to bind this application against", required = false)
	int port = this.getDefaultPort();

	@Parameter(names = {"-kb", "--runningInKubernetes"}, description = "The application is running in Kubernetes", required = false, arity = 1)
	boolean runningInKubernetes = SystemConfigurationSingleton.get().isRunningInKubernetes();

	@Parameter(names = {"-w", "--numWorkers"}, description = "The number of workers (indexers/validators) to start locally; should be at least one if the algorithm is started standalone (otherwise there are no workers to run the discovery)", required = false)
	int numWorkers = SystemConfigurationSingleton.get().getNumWorkers();

	public static void applyOn(String[] args) {
		CommandMaster commandMaster = new CommandMaster();
		CommandWorker commandWorker = new CommandWorker();
		JCommander jCommander = JCommander.newBuilder()
				.addCommand(SystemConfiguration.MASTER_ROLE, commandMaster)
				.addCommand(SystemConfiguration.WORKER_ROLE, commandWorker)
				.build();

		try {
			jCommander.parse(args);

			if (jCommander.getParsedCommand() == null)
				throw new ParameterException("No command given.");

			switch (jCommander.getParsedCommand()) {
				case SystemConfiguration.MASTER_ROLE:
					SystemConfigurationSingleton.get().update(commandMaster);
					InputConfigurationSingleton.get().update(commandMaster);
					break;
				case SystemConfiguration.WORKER_ROLE:
					SystemConfigurationSingleton.get().update(commandWorker);
					InputConfigurationSingleton.set(null);
					break;
				default:
					throw new AssertionError();
			}
		} catch (ParameterException e) {
			System.out.printf("Could not parse args: %s\n", e.getMessage());
			jCommander.usage();
			System.exit(1);
		}
	}
}