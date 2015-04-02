package javaposse.jobdsl.dsl.jobs

import com.google.common.base.Preconditions
import javaposse.jobdsl.dsl.ConfigFileType
import javaposse.jobdsl.dsl.ContextHelper
import javaposse.jobdsl.dsl.DslContext
import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.WithXmlAction
import javaposse.jobdsl.dsl.helpers.LocalRepositoryLocation
import javaposse.jobdsl.dsl.helpers.common.MavenContext
import javaposse.jobdsl.dsl.helpers.publisher.MavenPublisherContext
import javaposse.jobdsl.dsl.helpers.step.StepContext
import javaposse.jobdsl.dsl.helpers.triggers.MavenTriggerContext
import javaposse.jobdsl.dsl.helpers.triggers.TriggerContext
import javaposse.jobdsl.dsl.helpers.wrapper.MavenWrapperContext
import javaposse.jobdsl.dsl.helpers.wrapper.WrapperContext

class MavenJob extends Job {
    private final List<String> mavenGoals = []
    private final List<String> mavenOpts = []

    MavenJob(JobManagement jobManagement) {
        super(jobManagement)
    }

    @Override
    void steps(@DslContext(StepContext) Closure closure) {
        throw new IllegalStateException('steps cannot be applied for Maven jobs')
    }

    @Override
    void triggers(@DslContext(MavenTriggerContext) Closure closure) {
        TriggerContext context = new MavenTriggerContext(withXmlActions, jobManagement)
        ContextHelper.executeInContext(closure, context)

        withXmlActions << WithXmlAction.create { Node project ->
            context.triggerNodes.each {
                project / 'triggers' << it
            }
        }
    }

    @Override
    void wrappers(@DslContext(MavenWrapperContext) Closure closure) {
        WrapperContext context = new MavenWrapperContext(jobManagement)
        ContextHelper.executeInContext(closure, context)

        withXmlActions << WithXmlAction.create { Node project ->
            context.wrapperNodes.each {
                project / 'buildWrappers' << it
            }
        }
    }

    @Override
    void publishers(@DslContext(MavenPublisherContext) Closure closure) {
        MavenPublisherContext context = new MavenPublisherContext(jobManagement)
        ContextHelper.executeInContext(closure, context)

        withXmlActions << WithXmlAction.create { Node project ->
            context.publisherNodes.each {
                project / 'publishers' << it
            }
        }
    }

    /**
     * Specifies the path to the root POM.
     *
     * @param rootPOM path to the root POM
     */
    void rootPOM(String rootPOM) {
        withXmlActions << WithXmlAction.create { Node project ->
            Node node = methodMissing('rootPOM', rootPOM)
            project / node
        }
    }

    /**
     * Specifies the goals to execute.
     *
     * @param goals the goals to execute
     */
    void goals(String goals) {
        if (mavenGoals.empty) {
            withXmlActions << WithXmlAction.create { Node project ->
                Node node = methodMissing('goals', this.mavenGoals.join(' '))
                project / node
            }
        }
        mavenGoals << goals
    }

    /**
     * Specifies the JVM options needed when launching Maven as an external process.
     *
     * @param mavenOpts JVM options needed when launching Maven
     */
    void mavenOpts(String mavenOpts) {
        if (this.mavenOpts.empty) {
            withXmlActions << WithXmlAction.create { Node project ->
                Node node = methodMissing('mavenOpts', this.mavenOpts.join(' '))
                project / node
            }
        }
        this.mavenOpts << mavenOpts
    }

    /**
     * If set, Jenkins will send an e-mail notifications for each module, defaults to <code>false</code>.
     *
     * @param perModuleEmail set to <code>true</code> to enable per module e-mail notifications
     */
    @Deprecated
    void perModuleEmail(boolean perModuleEmail) {
        jobManagement.logDeprecationWarning()

        withXmlActions << WithXmlAction.create { Node project ->
            Node node = methodMissing('perModuleEmail', perModuleEmail)
            project / node
        }
    }

    /**
     * If set, Jenkins  will not automatically archive all artifacts generated by this project, defaults to
     * <code>false</code>.
     *
     * @param archivingDisabled set to <code>true</code> to disable automatic archiving
     */
    void archivingDisabled(boolean archivingDisabled) {
        withXmlActions << WithXmlAction.create { Node project ->
            Node node = methodMissing('archivingDisabled', archivingDisabled)
            project / node
        }
    }

    /**
     * Set to allow Jenkins to configure the build process in headless mode, defaults to <code>false</code>.
     *
     * @param runHeadless set to <code>true</code> to run the build process in headless mode
     */
    void runHeadless(boolean runHeadless) {
        withXmlActions << WithXmlAction.create { Node project ->
            Node node = methodMissing('runHeadless', runHeadless)
            project / node
        }
    }

    /**
     * Set to use isolated local Maven repositories.
     *
     * @param location the local repository to use for isolation
     */
    @Deprecated
    void localRepository(MavenContext.LocalRepositoryLocation location) {
        jobManagement.logDeprecationWarning()

        Preconditions.checkNotNull(location, 'localRepository can not be null')

        localRepository(location.location)
    }

    /**
     * Set to use isolated local Maven repositories.
     *
     * @param location the local repository to use for isolation
     */
    void localRepository(LocalRepositoryLocation location) {
        Preconditions.checkNotNull(location, 'localRepository can not be null')

        withXmlActions << WithXmlAction.create { Node project ->
            Node node = methodMissing('localRepository', [class: location.type])
            project / node
        }
    }

    void preBuildSteps(@DslContext(StepContext) Closure preBuildClosure) {
        StepContext preBuildContext = new StepContext(jobManagement)
        ContextHelper.executeInContext(preBuildClosure, preBuildContext)

        withXmlActions << WithXmlAction.create { Node project ->
            preBuildContext.stepNodes.each {
                project / 'prebuilders' << it
            }
        }
    }

    void postBuildSteps(@DslContext(StepContext) Closure postBuildClosure) {
        StepContext postBuildContext = new StepContext(jobManagement)
        ContextHelper.executeInContext(postBuildClosure, postBuildContext)

        withXmlActions << WithXmlAction.create { Node project ->
            postBuildContext.stepNodes.each {
                project / 'postbuilders' << it
            }
        }
    }

    void mavenInstallation(String name) {
        Preconditions.checkNotNull(name, 'name can not be null')

        withXmlActions << WithXmlAction.create { Node project ->
            project / 'mavenName'(name)
        }
    }

    void providedSettings(String settingsName) {
        String settingsId = jobManagement.getConfigFileId(ConfigFileType.MavenSettings, settingsName)
        Preconditions.checkNotNull(settingsId, "Managed Maven settings with name '${settingsName}' not found")

        withXmlActions << WithXmlAction.create { Node project ->
            project / settings(class: 'org.jenkinsci.plugins.configfiles.maven.job.MvnSettingsProvider') {
                settingsConfigId(settingsId)
            }
        }
    }
}
