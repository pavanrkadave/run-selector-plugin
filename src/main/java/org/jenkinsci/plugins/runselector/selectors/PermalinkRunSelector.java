/*
 * The MIT License
 *
 * Copyright (c) 2010-2011, InfraDNA, Inc., Alan Harder
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
import hudson.RelativePath;
import hudson.Util;
import hudson.model.Job;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.model.Run;
import hudson.util.ComboBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.runselector.RunSelectorDescriptor;
import org.jenkinsci.plugins.runselector.context.RunSelectorContext;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Picks up a build through {@link Permalink}.
 *
 * @author Kohsuke Kawaguchi
 */
public class PermalinkRunSelector extends AbstractSpecificRunSelector {

    @Nonnull
    private final String id;

    @DataBoundConstructor
    public PermalinkRunSelector(String id) {
        this.id = Util.fixNull(id).trim();
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Override
    @CheckForNull
    public Run<?, ?> getBuild(@Nonnull Job<?, ?> job, @Nonnull RunSelectorContext context) {
        String resolvedId = context.getEnvVars().expand(id);
        if (resolvedId.startsWith("$")) {
            context.logDebug("Unresolved variable {0}", resolvedId);
            return null;
        }

        Permalink p = job.getPermalinks().get(resolvedId);
        if (p == null) {
            context.logDebug("No permalink found for {0}", resolvedId);
            return null;
        }

        Run<?, ?> run = p.resolve(job);
        if (run == null) {
            context.logDebug("No such build {0} in {1}", id, job.getFullName());
            return null;
        }

        return run;
    }

    @Symbol("permalink")
    @Extension
    public static class DescriptorImpl extends RunSelectorDescriptor {
        @Override
        public String getDisplayName() {
            return org.jenkinsci.plugins.runselector.Messages.PermalinkRunSelector_DisplayName();
        }

        public ComboBoxModel doFillIdItems(@AncestorInPath Job<?, ?> copyingJob, @RelativePath("..") @QueryParameter("projectName") String projectName) {
            Job<?, ?> j = null;
            Jenkins jenkins = Jenkins.getInstance();
            if (projectName != null && jenkins != null) {
                j = jenkins.getItem(projectName, copyingJob, Job.class);
            }
            ComboBoxModel r = new ComboBoxModel();
            for (Permalink p : j != null ? j.getPermalinks() : Permalink.BUILTIN) {
                r.add(p.getId());
            }
            return r;
        }
    }
}
