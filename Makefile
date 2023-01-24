BUILD_DIR := ./out/production/mijavco
SRC_DIR := ./src
TARGET := compiler.Mijavco

SRCS := $(shell find $(SRC_DIR) -name '*.java')

all: $(SRCS)
	@javac -d $(BUILD_DIR) $(SRCS)

install:
	@sh install.sh

run: all
	@java -cp $(BUILD_DIR) $(TARGET) $(args)
