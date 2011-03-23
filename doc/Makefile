# How to render a template
html/%.html : %.vm
	java -jar ../jgcgen.jar $< -o $@

%.ngc : %.jgc
	java -jar ../jgcgen.jar -v $< -o $@

# Which templates to render
VMS=$(wildcard *.vm)
#HTMLS=$(VMS:.vm=.html)
HTMLS=$(VMS:%.vm=html/%.html)

# Which samples to generate
SAMPLES=$(wildcard samples/*.jgc)
NGCS=$(SAMPLES:%.jgc=%.ngc)

.PHONY: all clean

# Do everything
all: $(HTMLS) $(NGCS)

# Clean up
clean:
	rm -rf html
	rm -f samples/*.ngc

# Create output directory
html:
	mkdir html
	ln -s ../style html/style

# Render templates
$(HTMLS): html $(VMS)

# Generate samples
$(NGCS): $(SAMPLES)
