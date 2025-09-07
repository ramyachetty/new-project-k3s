package com.unbxd.platform.runner;

import com.unbxd.platform.config.CliOptions;
import io.cucumber.core.options.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;

import java.util.ArrayList;
import java.util.List;

@Command(
    name = "run-search-tests",
    version = "Search API Tests 1.0",
    mixinStandardHelpOptions = true,
    description = "Runs Cucumber tests for the Search API.")
public class CucumberTestRunner implements Runnable {

  @Option(
      names = {"-k", "--api-key"},
      required = true,
      description = "The API key for the search service.")
  private String apiKey;

  @Option(
      names = {"-s", "--site-key"},
      required = true,
      description = "The site key for the search service.")
  private String siteKey;

  @Option(
      names = {"-u", "--base-uri"},
      required = true,
      description = "The base URI for the API endpoint.")
  private String baseUri;

  @Option(
      names = {"--print-output"},
      description = "Print detailed Cucumber scenario and step output to the console.")
  private boolean printOutput = false;

  @Unmatched private List<String> cucumberOptions = new ArrayList<>();

  @Override
  public void run() {
    CliOptions.apiKey = this.apiKey;
    CliOptions.siteKey = this.siteKey;
    CliOptions.baseUri = this.baseUri;
    CliOptions.printOutput = this.printOutput;

    System.out.println("Base URI : " + CliOptions.baseUri);
    System.out.println("API Key  : " + CliOptions.apiKey);
    System.out.println("Site Key : " + CliOptions.siteKey);

    List<String> argsList = new ArrayList<>();
    if (CliOptions.printOutput) {
      System.out.println("Detailed Cucumber output enabled.");
      argsList.add("--plugin");
      argsList.add("pretty");
    } else {
      System.out.println("Detailed Cucumber output disabled. Only summary will be shown.");
      argsList.add("--plugin");
      argsList.add("summary");
    }

    argsList.add("--glue");
    argsList.add("com.unbxd.platform.stepdefs");
    argsList.add("src/test/resources/features");
    if (cucumberOptions != null) {
      argsList.addAll(cucumberOptions);
    }
    byte exitStatus =
        io.cucumber.core.cli.Main.run(
            argsList.toArray(new String[0]), Thread.currentThread().getContextClassLoader());

    System.exit(exitStatus);
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new CucumberTestRunner()).execute(args);
  }
}
