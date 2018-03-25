
import groovy.util.CliBuilder
import groovy.sql.Sql



def usage() {
    println """
    
    Sample usage as follows: 
    
    ${this.class.getName()} [options] DB_HOSTNAME:PORT:SID "QUERY_TO_EXECUTE_IN_DOUBLE_QUOTES" 
    
    """
}

def fatal(e) {
    println "XXX !!! FATAL !!! XXX"
    println "${this.class.getName()} threw an Exception: " + e.toString()
    
    System.exit(1)
}



class ConsoleColors {
    // Reset
    public static final String RESET = "\033[0m";  // Text Reset

    // Regular Colors
    public static final String BLACK = "\033[0;30m";   // BLACK
    public static final String RED = "\033[0;31m";     // RED
    public static final String GREEN = "\033[0;32m";   // GREEN
    public static final String YELLOW = "\033[0;33m";  // YELLOW
    public static final String BLUE = "\033[0;34m";    // BLUE
    public static final String PURPLE = "\033[0;35m";  // PURPLE
    public static final String CYAN = "\033[0;36m";    // CYAN
    public static final String WHITE = "\033[0;37m";   // WHITE

    // Bold
    public static final String BLACK_BOLD = "\033[1;30m";  // BLACK
    public static final String RED_BOLD = "\033[1;31m";    // RED
    public static final String GREEN_BOLD = "\033[1;32m";  // GREEN
    public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
    public static final String BLUE_BOLD = "\033[1;34m";   // BLUE
    public static final String PURPLE_BOLD = "\033[1;35m"; // PURPLE
    public static final String CYAN_BOLD = "\033[1;36m";   // CYAN
    public static final String WHITE_BOLD = "\033[1;37m";  // WHITE

    // Underline
    public static final String BLACK_UNDERLINED = "\033[4;30m";  // BLACK
    public static final String RED_UNDERLINED = "\033[4;31m";    // RED
    public static final String GREEN_UNDERLINED = "\033[4;32m";  // GREEN
    public static final String YELLOW_UNDERLINED = "\033[4;33m"; // YELLOW
    public static final String BLUE_UNDERLINED = "\033[4;34m";   // BLUE
    public static final String PURPLE_UNDERLINED = "\033[4;35m"; // PURPLE
    public static final String CYAN_UNDERLINED = "\033[4;36m";   // CYAN
    public static final String WHITE_UNDERLINED = "\033[4;37m";  // WHITE

    // Background
    public static final String BLACK_BACKGROUND = "\033[40m";  // BLACK
    public static final String RED_BACKGROUND = "\033[41m";    // RED
    public static final String GREEN_BACKGROUND = "\033[42m";  // GREEN
    public static final String YELLOW_BACKGROUND = "\033[43m"; // YELLOW
    public static final String BLUE_BACKGROUND = "\033[44m";   // BLUE
    public static final String PURPLE_BACKGROUND = "\033[45m"; // PURPLE
    public static final String CYAN_BACKGROUND = "\033[46m";   // CYAN
    public static final String WHITE_BACKGROUND = "\033[47m";  // WHITE

    // High Intensity
    public static final String BLACK_BRIGHT = "\033[0;90m";  // BLACK
    public static final String RED_BRIGHT = "\033[0;91m";    // RED
    public static final String GREEN_BRIGHT = "\033[0;92m";  // GREEN
    public static final String YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
    public static final String BLUE_BRIGHT = "\033[0;94m";   // BLUE
    public static final String PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
    public static final String CYAN_BRIGHT = "\033[0;96m";   // CYAN
    public static final String WHITE_BRIGHT = "\033[0;97m";  // WHITE

    // Bold High Intensity
    public static final String BLACK_BOLD_BRIGHT = "\033[1;90m"; // BLACK
    public static final String RED_BOLD_BRIGHT = "\033[1;91m";   // RED
    public static final String GREEN_BOLD_BRIGHT = "\033[1;92m"; // GREEN
    public static final String YELLOW_BOLD_BRIGHT = "\033[1;93m";// YELLOW
    public static final String BLUE_BOLD_BRIGHT = "\033[1;94m";  // BLUE
    public static final String PURPLE_BOLD_BRIGHT = "\033[1;95m";// PURPLE
    public static final String CYAN_BOLD_BRIGHT = "\033[1;96m";  // CYAN
    public static final String WHITE_BOLD_BRIGHT = "\033[1;97m"; // WHITE

    // High Intensity backgrounds
    public static final String BLACK_BACKGROUND_BRIGHT = "\033[0;100m";// BLACK
    public static final String RED_BACKGROUND_BRIGHT = "\033[0;101m";// RED
    public static final String GREEN_BACKGROUND_BRIGHT = "\033[0;102m";// GREEN
    public static final String YELLOW_BACKGROUND_BRIGHT = "\033[0;103m";// YELLOW
    public static final String BLUE_BACKGROUND_BRIGHT = "\033[0;104m";// BLUE
    public static final String PURPLE_BACKGROUND_BRIGHT = "\033[0;105m"; // PURPLE
    public static final String CYAN_BACKGROUND_BRIGHT = "\033[0;106m";  // CYAN
    public static final String WHITE_BACKGROUND_BRIGHT = "\033[0;107m";   // WHITE
}


String db_username, db_password
int row_offset = 1, max_rows = 50 
String[] column_names


def cli = new CliBuilder(usage: "${this.class.getName()} [options] [hostname] [\"sql_to_execute\"]",
    header: "Options\n\n", footer: "")
    
cli.help("Print this message")
cli.u(args: 1, longOpt: "user", required: true, argName: "username", "DB Username")
cli.p(args: 1, longOpt: "passwd", required: true, argName: "password", "DB Password")
cli.o(args: 1, longOpt: "offset", optionalArg: true, argName: "offset", "Row offset to start with. Default is 1")
cli.r(args: 1, longOpt: "rows", optionalArg: true, argName: "rows", "Max rows to fetch per query. Default is 50")
cli.k(args: 1, longOpt: "keys", valueSeparator: "," as char, optionalArg: true, argName: "field_1,...,field_N", "DB columns to ***highlight***")

def options = cli.parse(args)

if (!options || options.h) {
    usage()
    System.exit(1)
} else {
    db_username = options.u
    db_password = options.p
    row_offset = (options.o) ? options.o as int : 1
    max_rows = (options.r) ? options.r as int : 50
    column_names = (options.k) ? options.ks.join().split(',') : null

    db_host = options.arguments()[0]
    db_query = options.arguments()[1]
}


def db_connection_string = "jdbc:oracle:thin:${db_username}/PASS_VALUE@${db_host}"
def db_driver = "oracle.jdbc.driver.OracleDriver"

println()
println("### DB Connection: ${db_connection_string} | ${db_username} | ${db_password} ###")
if (column_names) println("==> Column names: ${column_names} ")
println("*** ARGS: ${options.arguments()} *** ")
println()



try  {
    Sql.withInstance(db_connection_string, db_username, db_password, db_driver) { sql ->
        
        sql.eachRow(db_query, row_offset, max_rows) { row ->
        
            def metadata = row.getMetaData()
            def columncount = metadata.getColumnCount()
            print "[ "
            (0 ..< metadata.getColumnCount()).each { i ->
                print "< "
                    
                    def _  = row.getAt(i)
                    if (_ instanceof oracle.sql.TIMESTAMP) { _ = _.stringValue() } 
                    if (column_names?.contains(metadata.getColumnName(i+1))) {
                        print("${ConsoleColors.RED_BOLD_BRIGHT + metadata.getColumnName(i+1) + ConsoleColors.RESET}:${ConsoleColors.RED_BOLD_BRIGHT + _ + ConsoleColors.RESET}")
                    } else {
                        print("${metadata.getColumnName(i+1)}:${_}")
                    }
                    
                print " > "
            }
            println("]")
        }
    
    }
} catch (e) {
    fatal(e.message)
}


System.exit(0)
