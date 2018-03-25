
import groovy.swing.*
import groovy.xml.*
import javax.swing.* 
import javax.swing.filechooser.*
import javax.swing.tree.*
import javax.swing.event.*
import java.awt.*
import java.awt.event.*

import org.slf4j.*
import org.apache.commons.lang3.*


logger = LoggerFactory.getLogger(this.class)


swing = new SwingBuilder()

dimension = Toolkit.getDefaultToolkit().getScreenSize()

root = new DefaultMutableTreeNode("Projects")
tree_model = new DefaultTreeModel(root)
proj_tree = new JTree(tree_model)
proj_tree.showsRootHandles = true
proj_tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION


invoker_process = null
selected_proj_files = null
selected_node = null


def tree_selection_handler = {
    def node = proj_tree.lastSelectedPathComponent.userObject
    swing.status_bar.text = "${proj_tree.selectionPath} - [${node.hasProperty('full_path') ? node.full_path : ''}][${node.hasProperty('testsuite_name') ? node.testsuite_name : ''}][${node.hasProperty('testcase_name') ? node.testcase_name : ''}]"
    selected_node = node
}
proj_tree.addTreeSelectionListener(tree_selection_handler as TreeSelectionListener)



class node {
    String full_path
    String testsuite_name
    String testcase_name
    String toString() {
        if (full_path && testsuite_name && testcase_name) return testcase_name
        if (full_path && testsuite_name) return testsuite_name
        if (full_path) return new File(full_path).name
        else return "!!!"
    }
}


def launch_file_chooser(fc) {
    int ret = fc.showOpenDialog()
    
    if (ret == JFileChooser.APPROVE_OPTION) {
        selected_proj_files = fc.getSelectedFiles()*.toString()
        
        assert selected_proj_files != null
        
        swing.status_bar.text = "You selected the following file" + (selected_proj_files.size()>1 ? "s: ${selected_proj_files}" : ": ${selected_proj_files}")
        
        selected_proj_files.each { file ->
            new_node = new DefaultMutableTreeNode(new node(full_path: file))

            swing.doOutside {
                def _ = parse_xml(file)
                
                swing.doLater {
                    _.each { tuple ->
                        // add testsuite first 
                        suite = new DefaultMutableTreeNode(new node(full_path: file, testsuite_name: tuple[0]))
                        // then add testcase
                        tuple[1].each { tc ->
                            suite.add(new DefaultMutableTreeNode(new node(full_path: file, testsuite_name: tuple[0], testcase_name: tc)))
                        }
                        new_node.add(suite)
                    }
                    
                    tree_model.insertNodeInto(new_node, root, root.getChildCount())
                    proj_tree.scrollPathToVisible(new TreePath(new_node.getPath()))
                }
            }
        }
    }
}


def parse_xml(xml_file) {
    def project = new XmlSlurper().parse(xml_file).declareNamespace([con: 'http://eviware.com/soapui/config'])
    def M = []
    for (testsuite in project.testSuite) {
        def L = []
        for (testcase in testsuite.testCase) { L << testcase.@name }
        M << [testsuite.@name, L]
    }
    M
}


def run_cli() {
    if (!selected_node || selected_node instanceof String) return 
   
    swing.console_output.append("!!! Selected node: " + (selected_node ? "null" : selected_node.toString()) + selected_node.class+ "!!!\n")
    
    def fw_path = swing.framework_home_dir.text
    def cwd = new File(selected_node.full_path).parent
    
    swing.run.enabled = false
    swing.stop.enabled = true
    
    //
    // For detailed info on cli parameters see http://readyapi.smartbear.com/features/automation/testrunner/cli 
    //
    def cli = [
        "${fw_path}" + "${fw_path.endsWith('/') ? '' : '/'}" + "../../bin/ebiz_testrunner" + "${SystemUtils.IS_OS_WINDOWS ? '.bat' : ''}",
        "${new File(selected_node.full_path).name}",
    ]
    
    def _ = ''
    if (selected_node.testsuite_name) {
        _ += "## " + selected_node.testsuite_name   
    }
    if (selected_node.testcase_name) {
        _ += ",@@ " + selected_node.testcase_name
    }
    
    cli << _
    
    if (swing.env.text != "") {
        cli << swing.env.text
    }
    
    def header = "${'*'*30} ${new Date()} ${'*'*30}"
    swing.console_output.append(header + "\n")
    swing.console_output.append("===> cli: (${cli}), cwd: [${cwd}] <===" + "\n")
    
    logger.info("===> cli: ${cli}, cwd: [${cwd}] <===")
    logger.info("""*** cmd: (${cli*.replaceAll(" ","\\\\ ").join(' ')}) ***""")
    
    swing.doOutside {
        invoker_process = cli.execute(null, new File(cwd)) 
        invoker_process.in.eachLine { line ->
            logger.info(line)
            
            swing.doLater {
                swing.console_output.append(line + "\n")
            }
        }
        invoker_process.out.close()
        invoker_process.waitFor()
        swing.doLater {
            swing.console_output.append('*' * header.size() + "\n")
            swing.run.enabled = true
            swing.stop.enabled = false
        }
    }

}


def scroll_bottom() {
    swing.console_output.caretPosition = swing.console_output.document.length
}


def stop_cli() {
    swing.console_output.append("!!!! STOPPING !!!!\n\n\n")
    
    if (invoker_process) {
        invoker_process.destroy()
        invoker_process.waitFor()
    }
    
    swing.run.enabled = true
    swing.stop.enabled = false
    
    scroll_bottom()
}


swing.edt {
    lookAndFeel 'nimbus'
    
    fc = fileChooser( 
            dialogTitle: "SoapUI Project xml",
            fileSelectionMode: JFileChooser.FILES_ONLY, 
            multiSelectionEnabled: true, 
            fileFilter: [getDescription: {-> "*.xml"}, accept: {file -> file.toString() ==~ /.*?\.xml/ || file.isDirectory()}] as FileFilter
    )
    
    frame(title: 'SoapUI CLI Runner', show: true, size: [(dimension.width) as int, (dimension.height) as int], defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
        borderLayout()
        
        toolBar(constraints: BorderLayout.NORTH, floatable: false, rollover: true) {
            button(text: 'Load SoapUI Projects', actionPerformed: { launch_file_chooser(fc) } )
            textField(text: 'Enter search term here')
        }
        
        splitPane(constraints: BorderLayout.CENTER, orientation: JSplitPane.HORIZONTAL_SPLIT, oneTouchExpandable:true, dividerLocation:0.3) {
            scrollPane(preferredSize: new Dimension(width: dimension.width*0.3, height: dimension.height)) { widget(proj_tree) }
            
            panel() {
                borderLayout()
                
                splitPane(constraints: BorderLayout.CENTER, orientation: JSplitPane.VERTICAL_SPLIT, oneTouchExpandable:true) {
                   panel() {
                       gridLayout(rows: 4, columns: 2)
                       
                       label(text: 'SoapUI/ReadyAPI folder location')
                       textField(id: 'framework_home_dir', columns: 40, text: "${System.getenv('FRAMEWORK_HOME')}" + "/3rd_party/ReadyAPI-1.9.0/")
                       
                       label(text: 'Output logs folder location')
                       textField(id: 'logs_dir', columns: 40, text: ((SystemUtils.IS_OS_WINDOWS) ? "pwd.bat".execute().text : "${System.getenv('PWD')}") + '/SOAPUI_LOGS')
                       
                       label(text: 'Environment')
                       textField(id: 'env', text: "")
                       
                       panel()
                       panel() {
                           button(id: 'run', text: 'Run', actionPerformed: { run_cli() } )
                           button(id: 'stop', text: 'Stop', actionPerformed: { stop_cli() })
                       }
                   }
                   
                   scrollPane() { 
                       txtarea = textArea(id: 'console_output', text: '', editable: false) 
                       txtarea.addKeyListener([
                           keyPressed: {
                               doLater {
                                   scroll_bottom()
                               }
                           },
                           keyTyped: {},
                           keyReleased: {},
                       ] as KeyListener)
                   }
                }
            }
        }
        
        panel(constraints: BorderLayout.SOUTH, border: BorderFactory.createBevelBorder(3), layout: new GridLayout(0,1)) {
            label(id:'status_bar', text: 'Status Bar')
        }
    }
    
    
}