package bazel.messages.handlers

import bazel.HandlerPriority
import bazel.Verbosity
import bazel.atLeast
import bazel.events.BuildStatus
import bazel.events.InvocationAttemptFinished
import bazel.messages.Color
import bazel.messages.ServiceMessageContext
import bazel.messages.apply

class InvocationAttemptFinishedHandler : EventHandler {
    override val priority: HandlerPriority
        get() = HandlerPriority.Low

    override fun handle(ctx: ServiceMessageContext) =
            if (ctx.event.payload is InvocationAttemptFinished) {
                ctx.onNext(ctx.messageFactory.createFlowFinished(ctx.event.payload.streamId.invocationId))
                if (ctx.verbosity.atLeast(Verbosity.Detailed)) {
                    if (ctx.event.payload.invocationResult.status == BuildStatus.CommandSucceeded) {
                        val description = "Invocation attempt completed"
                        ctx.onNext(ctx.messageFactory.createBuildStatus(description))
                        if (ctx.verbosity.atLeast(Verbosity.Detailed)) {
                            ctx.onNext(ctx.messageFactory.createMessage(
                                    ctx.buildMessage()
                                            .append(description.apply(Color.BuildStage))
                                            .append(", exit code: ${ctx.event.payload.exitCode}", Verbosity.Verbose)
                                            .toString()))
                        }
                    } else {
                        ctx.onNext(ctx.messageFactory.createErrorMessage(
                                ctx.buildMessage(false)
                                        .append("Invocation attempt failed")
                                        .append(": \"${ctx.event.payload.invocationResult.status.description}\"", Verbosity.Detailed)
                                        .append(", exit code: ${ctx.event.payload.exitCode}", Verbosity.Verbose)
                                        .toString()))
                    }
                }

                true
            } else ctx.handlerIterator.next().handle(ctx)
}