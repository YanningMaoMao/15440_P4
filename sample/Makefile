all: Server.class UserNode.class MessageConvert.class CommitInfo.class MessageContent.class MessageType.class IOHelper.class FullCommitProcess.class PhaseOneAbort.class PhaseTwoRecover.class CommitProcess.class CommitDecision.class SourceFileStatus.class

%.class: %.java
	javac $<

clean:
	rm -f *.class
