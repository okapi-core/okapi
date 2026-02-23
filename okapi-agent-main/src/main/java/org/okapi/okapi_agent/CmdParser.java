package org.okapi.okapi_agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.okapi.okapi_agent.config.GatewayConfig;
import org.okapi.okapi_agent.jobhandler.HandlerCfg;
import org.okapi.okapi_agent.jobhandler.PendingJobRunner;
import picocli.CommandLine;

@Slf4j
public class CmdParser {

  public record StaticConfig(
      HandlerCfg handlerCfg,
      GatewayConfig gatewayConfig,
      PendingJobRunner.PendingJobConfig pendingJobConfig) {}

  @CommandLine.Command(
      name = "okapi-agent",
      mixinStandardHelpOptions = true,
      description = "Okapi Agent")
  private static class CliArgs implements Runnable {
    @CommandLine.Option(
        names = {"ge", "--gateway-endpoint"},
        description = "Gateway endpoint URL",
        required = true)
    private String gatewayEndpoint;

    @CommandLine.Option(
        names = {"gtp", "--gateway-token-path"},
        description = "Path to gateway token",
        required = true)
    private String gatewayTokenPath;

    @CommandLine.Option(
        names = {"sources", "--sources-config-path"},
        description = "Path to sources configuration file",
        required = true)
    private String sourcesConfigPath;

    @CommandLine.Option(
        names = {"poll-delay", "--poll-delay"},
        description = "Duration between polling for new jobs in milliseconds",
        required = false)
    long delay = 5000;

    @Override
    public void run() {}
  }

  public static StaticConfig fromArgs(String[] args) {
    var cliArgs = new CliArgs();
    CommandLine cmd = new CommandLine(cliArgs);
    try {
      // handle --help / --version like a normal CLI
      cmd.parseArgs(args);
      if (cmd.isUsageHelpRequested()) {
        cmd.usage(System.out);
        System.exit(0);
      }
      if (cmd.isVersionHelpRequested()) {
        cmd.printVersionHelp(System.out);
        System.exit(0);
      }
    } catch (CommandLine.ParameterException e) {
      System.out.println(e.getMessage());
      System.exit(1);
      cmd.usage(System.err);
      return null;
    }

    var tokenPath = Path.of(cliArgs.gatewayTokenPath);
    if (!Files.exists(tokenPath)) {
      cmd.getOut().println("Gateway token file does not exist: " + cliArgs.gatewayTokenPath);
    }
    try {
      return new StaticConfig(
          new HandlerCfg(Path.of(cliArgs.sourcesConfigPath)),
          new GatewayConfig(cliArgs.gatewayEndpoint, Files.readString(tokenPath).trim()),
          new PendingJobRunner.PendingJobConfig(cliArgs.delay));
    } catch (IOException e) {
      System.out.println("Failed to read gateway token file: " + e.getMessage());
      System.exit(2);
      return null;
    }
  }
}
