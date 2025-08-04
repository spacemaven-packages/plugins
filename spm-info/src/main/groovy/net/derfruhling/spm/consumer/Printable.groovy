package net.derfruhling.spm.consumer

trait Printable {
    void prettyPrintTo(IndentWriter writer) {
        this.metaPropertyValues.each { field ->
            writer.writeProperty(field.name, field.value)
        }
    }
}
