BUILD_DIR := ./out/production/mijavco
SRC_DIR := ./src
TARGET := compiler.Main

SRCS := $(shell find $(SRC_DIR) -name '*.java')

all: $(SRCS)
	@javac -d $(BUILD_DIR) $(SRCS)

run: all
	@java -cp $(BUILD_DIR) $(TARGET) $(args)



