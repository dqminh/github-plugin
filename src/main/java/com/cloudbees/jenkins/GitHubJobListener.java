package com.cloudbees.jenkins;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Build;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.git.GitChangeSet;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.logging.Logger;

@Extension
public class GitHubJobListener extends RunListener<Run> {
    private static final Logger LOGGER = Logger.getLogger(GitHubJobListener.class.getName());

    static {
        LOGGER.info("INITIALIZING");
    }

    public GitHubJobListener() {
        super(Run.class);
        LOGGER.info("CREATING");
    }

    @Override
    public void onStarted(Run r, TaskListener listener) {
        LOGGER.info("STARTED");
    }

    @Override
    public void onCompleted(Run r, TaskListener listener) {
        Build build = (Build) r;
        Result result = build.getResult();

        GithubProjectProperty property = (GithubProjectProperty)
                build.getParent().getProperty(GithubProjectProperty.class);

        if (property != null) {
            String projectUrl = property.getProjectUrl().toString();
            if (projectUrl.endsWith("/")) {
                projectUrl = projectUrl.substring(0, projectUrl.length() - 1);
            }
            GitHubRepositoryName repoName = GitHubRepositoryName.create(projectUrl);
            try {
                EnvVars env = build.getEnvironment(listener);
                String commitId = env.get("GIT_COMMIT");
                GHRepository repo = repoName.resolveOne();

                if (result == Result.SUCCESS) {
                    repo.createCommitStatus(commitId, GHCommitState.SUCCESS, build.getUrl(), build.getDescription());
                } else {
                    repo.createCommitStatus(commitId, GHCommitState.FAILURE, build.getUrl(), build.getDescription());
                }
            } catch (IOException e) {
                LOGGER.info("Error: " + e.getStackTrace());
            } catch (InterruptedException e) {
                LOGGER.info("Error: " + e.getStackTrace());
            }
        }
    }

    @Override
    public void onFinalized(Run r) {
        Result result = r.getResult();
        LOGGER.info("FINALIZED: " + result.toString());
    }

    private String getStatus(Run r) {
        Result result = r.getResult();
        String status = null;
        if (result != null) {
            status = result.toString();
        }
        return status;
    }
}
