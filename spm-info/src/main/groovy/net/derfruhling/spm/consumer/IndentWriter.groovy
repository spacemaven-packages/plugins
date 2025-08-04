package net.derfruhling.spm.consumer

class IndentWriter {
    private int indentation = 0
    private final PrintWriter delegate

    IndentWriter(PrintWriter delegate) {
        this.delegate = delegate
    }

    IndentWriter(PrintWriter delegate, int indentation) {
        this.delegate = delegate
        this.indentation = indentation
    }

    void indent(Closure c) {
        indentation += 2

        try {
            c.run()
        } finally {
            indentation -= 2
        }
    }

    void printIndent() {
        delegate.print(" ".repeat(indentation))
    }

    void writeProperty(Object name, Object value) {
        printIndent()
        delegate.print("$name: ")
        writeValueInternal(value)
    }

    void writeProperty(Map<?, ?> map) {
        map.forEach { k, v -> writeProperty(k, v) }
    }

    void writeValue(value) {
        printIndent()
        writeValueInternal(value)
    }

    private void writeValueInternal(value) {
        if (value instanceof String) {
            delegate.println(value)
        } else if (value instanceof Number) {
            delegate.println(value)
        } else if (value instanceof Collection) {
            delegate.println("[ // ${value.size()} elements")
            indent {
                for (def entry in value.indexed()) {
                    writeProperty(entry.key, entry.value)
                }
            }
            printIndent()
            delegate.println("] // ${value.size()} elements")
        } else if (value instanceof Map) {
            delegate.println("[ // ${value.size()} entries")
            indent {
                for (def entry in value) {
                    writeProperty(entry.key, entry.value)
                }
            }
            printIndent()
            delegate.println("] // ${value.size()} entries")
        } else if (value instanceof Printable) {
            delegate.println(value.class.name + " [")
            indent {
                (value as Printable).prettyPrintTo(this)
            }
            printIndent()
            delegate.println("] // ${value.class.name}")
        } else {
            delegate.println(value.toString())
        }
    }
}