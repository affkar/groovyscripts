import java.util.regex.Matcher;
public class SystemCommandExecutor
{
  private List<String> commandInformation;
  private String adminPassword;
  private ThreadedStreamHandler inputStreamHandler;
  private ThreadedStreamHandler errorStreamHandler;
  
  /**
   * Pass in the system command you want to run as a List of Strings, as shown here:
   * 
   * List<String> commands = new ArrayList<String>();
   * commands.add("/sbin/ping");
   * commands.add("-c");
   * commands.add("5");
   * commands.add("www.google.com");
   * SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
   * commandExecutor.executeCommand();
   * 
   * Note: I've removed the other constructor that was here to support executing
   *       the sudo command. I'll add that back in when I get the sudo command
   *       working to the point where it won't hang when the given password is
   *       wrong.
   *
   * @param commandInformation The command you want to run.
   */
  public SystemCommandExecutor(final List<String> commandInformation)
  {
    if (commandInformation==null) throw new NullPointerException("The commandInformation is required.");
    this.commandInformation = commandInformation;
    this.adminPassword = null;
  }

  public int executeCommand()
  throws IOException, InterruptedException
  {
    int exitValue = -99;

    try
    {
      ProcessBuilder pb = new ProcessBuilder(commandInformation);
      Process process = pb.start();

      // you need this if you're going to write something to the command's input stream
      // (such as when invoking the 'sudo' command, and it prompts you for a password).
      OutputStream stdOutput = process.getOutputStream();
      
      // i'm currently doing these on a separate line here in case i need to set them to null
      // to get the threads to stop.
      // see http://java.sun.com/j2se/1.5.0/docs/guide/misc/threadPrimitiveDeprecation.html
      InputStream inputStream = process.getInputStream();
      InputStream errorStream = process.getErrorStream();

      // these need to run as java threads to get the standard output and error from the command.
      // the inputstream handler gets a reference to our stdOutput in case we need to write
      // something to it, such as with the sudo command
      inputStreamHandler = new ThreadedStreamHandler(inputStream, stdOutput, adminPassword);
      errorStreamHandler = new ThreadedStreamHandler(errorStream);

      // TODO the inputStreamHandler has a nasty side-effect of hanging if the given password is wrong; fix it
      inputStreamHandler.start();
      errorStreamHandler.start();

      // TODO a better way to do this?
      exitValue = process.waitFor();
 
      // TODO a better way to do this?
      inputStreamHandler.interrupt();
      errorStreamHandler.interrupt();
      inputStreamHandler.join();
      errorStreamHandler.join();
    }
    catch (IOException e)
    {
      // TODO deal with this here, or just throw it?
      throw e;
    }
    catch (InterruptedException e)
    {
      // generated by process.waitFor() call
      // TODO deal with this here, or just throw it?
      throw e;
    }
    finally
    {
      return exitValue;
    }
  }

  /**
   * Get the standard output (stdout) from the command you just exec'd.
   */
  public StringBuilder getStandardOutputFromCommand()
  {
    return inputStreamHandler.getOutputBuffer();
  }

  /**
   * Get the standard error (stderr) from the command you just exec'd.
   */
  public StringBuilder getStandardErrorFromCommand()
  {
    return errorStreamHandler.getOutputBuffer();
  }


}


class ThreadedStreamHandler extends Thread
{
  InputStream inputStream;
  String adminPassword;
  OutputStream outputStream;
  PrintWriter printWriter;
  StringBuilder outputBuffer = new StringBuilder();
  private boolean sudoIsRequested = false;
  
  /**
   * A simple constructor for when the sudo command is not necessary.
   * This constructor will just run the command you provide, without
   * running sudo before the command, and without expecting a password.
   * 
   * @param inputStream
   * @param streamType
   */
  ThreadedStreamHandler(InputStream inputStream)
  {
    this.inputStream = inputStream;
  }

  /**
   * Use this constructor when you want to invoke the 'sudo' command.
   * The outputStream must not be null. If it is, you'll regret it. :)
   * 
   * TODO this currently hangs if the admin password given for the sudo command is wrong.
   * 
   * @param inputStream
   * @param streamType
   * @param outputStream
   * @param adminPassword
   */
  ThreadedStreamHandler(InputStream inputStream, OutputStream outputStream, String adminPassword)
  {
    this.inputStream = inputStream;
    this.outputStream = outputStream;
    this.printWriter = new PrintWriter(outputStream);
    this.adminPassword = adminPassword;
    this.sudoIsRequested = true;
  }
  
  public void run()
  {
    // on mac os x 10.5.x, when i run a 'sudo' command, i need to write
    // the admin password out immediately; that's why this code is
    // here.
    if (sudoIsRequested)
    {
      //doSleep(500);
      printWriter.println(adminPassword);
      printWriter.flush();
    }

    BufferedReader bufferedReader = null;
    try
    {
      bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      String line = null;
      while ((line = bufferedReader.readLine()) != null)
      {
        outputBuffer.append(line + "\n");
      }
    }
    catch (IOException ioe)
    {
      // TODO handle this better
      ioe.printStackTrace();
    }
    catch (Throwable t)
    {
      // TODO handle this better
      t.printStackTrace();
    }
    finally
    {
      try
      {
        bufferedReader.close();
      }
      catch (IOException e)
      {
        // ignore this one
      }
    }
  }
  
  private void doSleep(long millis)
  {
    try
    {
      Thread.sleep(millis);
    }
    catch (InterruptedException e)
    {
      // ignore
    }
  }
  
  public StringBuilder getOutputBuffer()
  {
    return outputBuffer;
  }

}

class BranchPairGenerator{

    
def modulesAsString=''''''
   

    List<Pair> getBranchPairs(String branchName1, String branchName2){
        List<Pair> pairs=[]
		//int i=0
        for(module in modulesAsString.split("\n")){
            pairs << new Pair("${module}/branches/${branchName1}","${module}/branches/${branchName2}")
			//i++
        }
        return pairs;
    }    

}

class Pair{
    def branch1, branch2;
    Pair(def branch1, def branch2){
        this.branch1=branch1
        this.branch2=branch2
    }
    
    String toString(){
        return "new Pair('$branch1', '$branch2')"
    }
	
	boolean equals(def obj){
		branch1==obj.branch1 && branch2==obj.branch2
	}
}




class MergeInfoEligible{

    def branch1, branch2;
    def revisionsEligibleToMerge=[]
	def revisionsEligibleToReverseMerge=[]
	
    MergeInfoEligible(def branch1, def branch2){
        this.branch1=branch1
        this.branch2=branch2
    }
    
    MergeInfoEligible(Pair pair){
        this.branch1=pair.branch1
        this.branch2=pair.branch2
    }
	
	boolean equals(def obj){
		branch1==obj.branch1 && branch2==obj.branch2
	}
    
    boolean allMerged(){
        return revisionsEligibleToMerge.length()==0
    }
    
    boolean allReverseMerged(){
        return revisionsEligibleToReverseMerge.length()==0
    }
	
	String printRevisionsEligibleToMerge(){
		revisionsEligibleToMerge()
		def cloneRevs=revisionsEligibleToMerge.clone();
		while(cloneRevs.size()>0){
			List<String> command2=["svn","log", branch1, "-v"]
			for (rev in cloneRevs.take(25)){
				command2 << "-r"
				command2 << rev
			}
			SystemCommandExecutor commandExecutor2 = new SystemCommandExecutor(command2);
			//println "Command to be executed is ${command2.join(" ")}"
			int result2 = commandExecutor2.executeCommand();
			
			// get the output from the command
			StringBuilder stdout2 = commandExecutor2.getStandardOutputFromCommand();
			StringBuilder stderr2 = commandExecutor2.getStandardErrorFromCommand();
			
			if(stderr2.toString()!=null && stderr2.toString() !=""){
				throw new Exception("${stderr2.toString()} when trying to execute ${command2.toString()}");
			}
			println stdout2.toString()
			cloneRevs=cloneRevs.drop(25)
		}
		List<String> command2=["svn","merge", branch1 ]
		revisionsEligibleToMerge.each{
			command2 << '-c'
			command2 << it
		}
		if(revisionsEligibleToMerge.size()>0){
			println "SVN Command to merge : ${command2.join(" ")}"
		}
	}
    
    String[] revisionsEligibleToMerge(){
        List<String> command = new ArrayList<String>();
        command.add("svn");
        command.add("mergeinfo");
        command.add("--show-revs");
        command.add("eligible");
        command.add(branch1);
        command.add(branch2);
        
		println "Command to be executed is ${command.join(" ")}"
		
        // execute my command
        SystemCommandExecutor commandExecutor = new SystemCommandExecutor(command);
        int result = commandExecutor.executeCommand();
        
        // get the output from the command
        StringBuilder stdout = commandExecutor.getStandardOutputFromCommand();
        StringBuilder stderr = commandExecutor.getStandardErrorFromCommand();
        
        // print the output from the command
        if(stderr.toString()!=null && stderr.toString() !=""){
            throw new Exception("${stderr.toString()} when trying to execute ${command.toString()}");
        }
		//println """Received stdout as ..${stdout}. Parsing now"""
        def revs=stdout.toString().split("\n");
		while(revs.size()>0){
			List<String> command2=["svn","log", branch1]
			for(rev in revs.take(25)){
				// execute my command
				command2 << "-r"
				command2 << rev
			}
			
			SystemCommandExecutor commandExecutor2 = new SystemCommandExecutor(command2);
			
			//println "Command to be executed is ${command2.join(" ")}"
			int result2 = commandExecutor2.executeCommand();
			
			// get the output from the command
			StringBuilder stdout2 = commandExecutor2.getStandardOutputFromCommand();
			StringBuilder stderr2 = commandExecutor2.getStandardErrorFromCommand();
			
			if(stderr2.toString()!=null && stderr2.toString() !=""){
				throw new Exception("${stderr2.toString()} when trying to execute ${command2.toString()}");
			}
			def line=stdout2.toString().split("\n");
			
			line.each{
				//println "output $it\n"
				if(it =~ /^(r\d*) \| (\w*) \|/){
					Matcher matcher=(it =~ /^(r\d*) \| (\w*) \|/)
					if( matcher.hasGroup() && matcher[0][2] != 'jenkins'){
						//println "matcher[0] ${matcher[0]}"
						revisionsEligibleToMerge << matcher[0][1]
					}
				}
			}
			
			revs=revs.drop(25)
		}
        return revisionsEligibleToMerge
    }
	
	void printMatches(def lines){
		lines.each{
					println "output $it\n"
					if(it =~ /^(r\d*) \| (\w*) \|/){
						println "If block"
						Matcher matcher=(it =~ /^(r\d*) \| (\w*) \|/)
						if( matcher.hasGroup() && matcher[0][2] != 'jenkins'){
							println "matcher[0] ${matcher[0][1]}"
							//revisionsEligibleToMerge << matcher[0][1]
						}
					}else{
						println "Else Block"
					}
				}
	}

    String[] revisionsEligibleToReverseMerge(){
        return revisionsEligibleToReverseMerge = new MergeInfoEligible(branch2, branch1).revisionsEligibleToMerge()
    }

    
}

