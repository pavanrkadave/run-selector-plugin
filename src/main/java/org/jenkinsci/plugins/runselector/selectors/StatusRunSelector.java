/*
 * The MIT License
 *
 * Copyright (c) 2004-2011, Sun Microsystems, Inc., Alan Harder
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.runselector.selectors;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import org.jenkinsci.plugins.runselector.RunSelector;
import org.jenkinsci.plugins.runselector.context.RunSelectorContext;
import org.jvnet.localizer.Localizable;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Select build based on the specific status build.
 * @author Alan Harder
 */
public class StatusRunSelector extends RunSelector {
    public enum BuildStatus {
        /** Stable builds.*/
        stable(org.jenkinsci.plugins.runselector.Messages._StatusRunSelector_BuildStatus_Stable()),

        /** Stable or Unstable builds.*/
        successful(org.jenkinsci.plugins.runselector.Messages._StatusRunSelector_BuildStatus_Successful()),

        /** Unstable builds. */
        unstable(org.jenkinsci.plugins.runselector.Messages._StatusRunSelector_BuildStatus_Unstable()),

        /** Failed builds. */
        failed(org.jenkinsci.plugins.runselector.Messages._StatusRunSelector_BuildStatus_Failed()),

        /** Completed builds with any build results.*/
        completed(org.jenkinsci.plugins.runselector.Messages._StatusRunSelector_BuildStatus_Completed()),

        /** Any builds including incomplete (running) ones.*/
        any(org.jenkinsci.plugins.runselector.Messages._StatusRunSelector_BuildStatus_Any());

        private final Localizable displayName;

        BuildStatus(Localizable displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName.toString();
        }
    }

    @Nonnull
    private final BuildStatus buildStatus;

    public StatusRunSelector() {
        this(BuildStatus.stable);
    }

    /**
     * @param buildStatus build status of the build to select
     */
    @DataBoundConstructor
    public StatusRunSelector(@CheckForNull BuildStatus buildStatus) {
        this.buildStatus = buildStatus != null ? buildStatus : BuildStatus.stable;
    }

    /**
     * @return build status to select
     */
    @Nonnull
    public BuildStatus getBuildStatus() {
        return buildStatus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CheckForNull
    public Run<?, ?> getNextBuild(@Nonnull Job<?, ?> job, @Nonnull RunSelectorContext context) {
        Run<?, ?> previousBuild = context.getLastMatchBuild();
        if (previousBuild == null) {
            // the first time
            switch(getBuildStatus()) {
                case stable:
                return job.getLastStableBuild();
                case unstable:
                return job.getLastUnstableBuild();
                case failed:
                return job.getLastFailedBuild();
                case successful:
                // really confusing, but in this case,
                // "successful" means marked as SUCCESS or UNSTABLE.
                return job.getLastSuccessfulBuild();
                case completed:
                return job.getLastCompletedBuild();
                case any:
                return job.getLastBuild();
            }
        } else {
            // the second or later time
            switch(getBuildStatus()) {
                case stable:
                // really confusing, but in this case,
                // "successful" means marked as SUCCESS.
                return previousBuild.getPreviousSuccessfulBuild();
                case unstable:
                for (
                        previousBuild = previousBuild.getPreviousBuild();
                        previousBuild != null;
                        previousBuild = previousBuild.getPreviousBuild()
                ) {
                    Result r = previousBuild.getResult();
                    if (Result.UNSTABLE.equals(r)) {
                        return previousBuild;
                    }
                }
                break;
                case failed:
                return previousBuild.getPreviousFailedBuild();
                case successful:
                for (
                        previousBuild = previousBuild.getPreviousBuild();
                        previousBuild != null;
                        previousBuild = previousBuild.getPreviousBuild()
                ) {
                    Result r = previousBuild.getResult();
                    if (r != null && r.isBetterOrEqualTo(Result.UNSTABLE)) {
                        return previousBuild;
                    }
                }
                break;
                case completed:
                return previousBuild.getPreviousCompletedBuild();
                case any:
                return previousBuild.getPreviousBuild();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return String.format(
                "%s (%s)",
                super.getDisplayName(),
                getBuildStatus().toString()
        );
    }

    @Extension(ordinal=100)
    public static final Descriptor<RunSelector> DESCRIPTOR =
            new SimpleRunSelectorDescriptor(
                StatusRunSelector.class, org.jenkinsci.plugins.runselector.Messages._StatusRunSelector_DisplayName());
}
