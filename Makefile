BUILD_DIR = ./build
DOC_DIR = ./docs
JFLAGS = -g -cp $(BUILD_DIR)
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java -d $(BUILD_DIR)

CLASSES = \
	src/main/java/pl/tivian/security/*.java \
	src/main/java/pl/tivian/util/*.java \
	src/test/java/pl/tivian/security/test/*.java


default: classes

classes: $(CLASSES:.java=.class)

test:
	@cd $(BUILD_DIR) && java pl.tivian.security.test.AuthTokenTest

.PHONY: doc
doc:
	@find ./src/main -type f -name "*.java" | xargs javadoc -d $(DOC_DIR)

clean:
	@echo Deleting class files...
	@find $(BUILD_DIR) -name "*.class" | xargs rm
	@echo done.