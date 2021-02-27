import java.io.*;
import java.util.Scanner;


public class Main {
	private static final PCBUtil pcb = PCBUtil.getPCB();
	private static final Resource R1 = new Resource(1,1);
	private static final Resource R2 = new Resource(2,2);
	private static final Resource R3 = new Resource(3,3);
	private static final Resource R4 = new Resource(4,4);

	public static void main(String[] args) throws IOException {
		pcb.createProcess("init", 0);
		System.out.print("init" + " ");
		if(args.length != 0) {//带参数则读取文件
			loadFile(args[0]);
		}else {//不带参数则执行扫描逐行读取
			System.out.println();
			Scanner scanner = new Scanner(System.in);
			while(scanner.hasNextLine()) {
				String input = scanner.nextLine();
				if(input.trim().equals("")) {
					continue;
				}
				exec(input);
			}
		}
	}
	
	public static void exec(String input) {
		String[] commands = new String[] {input};
		for(String command : commands) {//对不同的输入命令进行处理
			String[] cmd = command.split("\\s+");
			String option = cmd[0];
			switch(option) {
			case "cr":
				if(cmd.length != 3) {// 检查输入格式是否正确  
					System.out.println("参数格式错误!");
				}else{
					String processName = cmd[1];
					int priority = 0;
					try {// 检查优先级的输入是否正确 
						priority = Integer.parseInt(cmd[2]);
						if(priority <= 0 || priority > 2) {
							System.out.println("参数错误!");
							continue;
						}
					}catch(Exception e) {
						System.out.println("参数错误!");
					}
					if(pcb.existName(processName)) { // 检查用户输入的进程名是否已经存在  
						System.out.println("进程已存在!");
						break;
					}
					pcb.createProcess(processName, priority);
				}
				break;
				
			case "de":
				if(cmd.length != 2) { // 检查输入格式是否正确 
					System.out.println("参数格式错误!");
				}else{
					String processName = cmd[1];
					Process process = pcb.findProcess(processName);
					if(process == null) {// 检查用户输入的进程名是否存在 
						System.out.println("无此进程!");
					}else if(processName.equals("init"))// 设定不允许用户删除系统init进程
						System.out.println("无权限终止init进程!");
					else {
						process.destroy();
						process.killSubTree();
						pcb.scheduler();
					}
				}
				break;
			
			case "req":
				if(cmd.length != 3) {// 检查输入格式是否正确
					System.out.println("参数格式错误!");
				}else{
					String resourceName = cmd[1];
					int needNum = 0;
					try {
						needNum = Integer.parseInt(cmd[2]);
					}catch(Exception e) {
						System.out.println("参数错误!");
					}
					Process currentProcess = pcb.getCurrentProcess();// 获取当前进程 
					switch(resourceName) {// 检查资源名称，请求对应资源
					case "R1":
						R1.request(currentProcess, needNum);
						break;
					case "R2":
						R2.request(currentProcess, needNum);
						break;
					case "R3":
						R3.request(currentProcess, needNum);
						break;
					case "R4":
						R4.request(currentProcess, needNum);
						break;
					default:
						System.out.println("错误输入!");
					}
				}
				break;
				
			case "rel":
				if(cmd.length != 3) {// 检查输入格式是否正确
					System.out.println("参数格式错误!");
				}else{
					String resourceName = cmd[1];
					int relNum = 0;
					try {
						relNum = Integer.parseInt(cmd[2]);
					}catch(Exception e) {
						System.out.println("参数错误!");
					}
					Process currentProcess = pcb.getCurrentProcess();
					switch(resourceName) { // 检查资源名称，释放对应资源
					case "R1":
						R1.release(currentProcess, relNum);
						break;
					case "R2":
						R2.release(currentProcess, relNum);
						break;
					case "R3":
						R3.release(currentProcess, relNum);
						break;
					case "R4":
						R4.release(currentProcess, relNum);
						break;
					}
					break;
				}
				
			case "to":
				pcb.timeout();
				break;
			
			case "lp":
				if(cmd.length == 1) { // lp命令打印所有进程树和信息
					pcb.printProcessTree(pcb.findProcess("init"), 0);
				}else if(cmd.length < 3 || !cmd[1].equals("-p")) {
					System.out.println("参数错误!");
				}else {
					String pname = cmd[2];
					Process process = pcb.findProcess(pname);
					if(process == null) {
						System.out.println("there is no process named" + pname);
					}else {
						pcb.printProcessDetail(process);
					}
				}
				break;
				
			case "lr":
				R1.printCurrentStatus();
				R2.printCurrentStatus();
				R3.printCurrentStatus();
				R4.printCurrentStatus();
				break;
				
			case "help":
				printHelp();
				break;
				
			case "exit":
				System.exit(0);
				
			default:
				System.out.println("命令错误!");
				break;
			}
		}
		if(pcb.getCurrentProcess() != null)
			System.out.print(pcb.getCurrentProcess().getProcessName() + " ");
	}
	
	//加载文件，逐行读取执行exec函数
	private static void loadFile(String filePath) throws IOException{
		InputStream in = new FileInputStream(filePath);
		LineNumberReader reader = new LineNumberReader(new FileReader(filePath));
		String cmd = null;
		while((cmd = reader.readLine()) != null) {
			if(!"".equals(cmd)) {
				exec(cmd);
			}
		}
	}
	
	//打印帮助信息
	private static void printHelp() {
		System.out.println("------------------------------------------------------------");
		System.out.println("    cr pname priority 创建新进程并指定进程名与优先级{0， 1， 2}");
		System.out.println("    de pname          根据进程名终止某进程（init进程除外）");
		System.out.println("    req rname num     根据资源名和数量为当前进程申请相应资源");
		System.out.println("    rel rname num     根据资源名和数量为当前进程释放相应资源");
		System.out.println("    to                时间片结束");
		System.out.println("    lp                打印所有进程信息");
		System.out.println("    lp -p pname       根据进程名打印特定进程信息");
		System.out.println("    lr                打印所有资源的信息");
		System.out.println("    exit              退出");
	}

}
