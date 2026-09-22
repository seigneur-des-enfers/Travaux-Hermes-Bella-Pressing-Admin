fun main() {
    val escapedLog = "foo\nbar"
    val json = "{\"fields\": {\"log\": {\"stringValue\": \"$escapedLog\"}}}"
    println(json)
}
