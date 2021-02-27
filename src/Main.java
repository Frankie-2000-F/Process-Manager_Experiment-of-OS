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
		if(args.length != 0) {//���������ȡ�ļ�
			loadFile(args[0]);
		}else {//����������ִ��ɨ�����ж�ȡ
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
		for(String command : commands) {//�Բ�ͬ������������д���
			String[] cmd = command.split("\\s+");
			String option = cmd[0];
			switch(option) {
			case "cr":
				if(cmd.length != 3) {// ��������ʽ�Ƿ���ȷ  
					System.out.println("������ʽ����!");
				}else{
					String processName = cmd[1];
					int priority = 0;
					try {// ������ȼ��������Ƿ���ȷ 
						priority = Integer.parseInt(cmd[2]);
						if(priority <= 0 || priority > 2) {
							System.out.println("��������!");
							continue;
						}
					}catch(Exception e) {
						System.out.println("��������!");
					}
					if(pcb.existName(processName)) { // ����û�����Ľ������Ƿ��Ѿ�����  
						System.out.println("�����Ѵ���!");
						break;
					}
					pcb.createProcess(processName, priority);
				}
				break;
				
			case "de":
				if(cmd.length != 2) { // ��������ʽ�Ƿ���ȷ 
					System.out.println("������ʽ����!");
				}else{
					String processName = cmd[1];
					Process process = pcb.findProcess(processName);
					if(process == null) {// ����û�����Ľ������Ƿ���� 
						System.out.println("�޴˽���!");
					}else if(processName.equals("init"))// �趨�������û�ɾ��ϵͳinit����
						System.out.println("��Ȩ����ֹinit����!");
					else {
						process.destroy();
						process.killSubTree();
						pcb.scheduler();
					}
				}
				break;
			
			case "req":
				if(cmd.length != 3) {// ��������ʽ�Ƿ���ȷ
					System.out.println("������ʽ����!");
				}else{
					String resourceName = cmd[1];
					int needNum = 0;
					try {
						needNum = Integer.parseInt(cmd[2]);
					}catch(Exception e) {
						System.out.println("��������!");
					}
					Process currentProcess = pcb.getCurrentProcess();// ��ȡ��ǰ���� 
					switch(resourceName) {// �����Դ���ƣ������Ӧ��Դ
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
						System.out.println("��������!");
					}
				}
				break;
				
			case "rel":
				if(cmd.length != 3) {// ��������ʽ�Ƿ���ȷ
					System.out.println("������ʽ����!");
				}else{
					String resourceName = cmd[1];
					int relNum = 0;
					try {
						relNum = Integer.parseInt(cmd[2]);
					}catch(Exception e) {
						System.out.println("��������!");
					}
					Process currentProcess = pcb.getCurrentProcess();
					switch(resourceName) { // �����Դ���ƣ��ͷŶ�Ӧ��Դ
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
				if(cmd.length == 1) { // lp�����ӡ���н���������Ϣ
					pcb.printProcessTree(pcb.findProcess("init"), 0);
				}else if(cmd.length < 3 || !cmd[1].equals("-p")) {
					System.out.println("��������!");
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
				System.out.println("�������!");
				break;
			}
		}
		if(pcb.getCurrentProcess() != null)
			System.out.print(pcb.getCurrentProcess().getProcessName() + " ");
	}
	
	//�����ļ������ж�ȡִ��exec����
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
	
	//��ӡ������Ϣ
	private static void printHelp() {
		System.out.println("------------------------------------------------------------");
		System.out.println("    cr pname priority �����½��̲�ָ�������������ȼ�{0�� 1�� 2}");
		System.out.println("    de pname          ���ݽ�������ֹĳ���̣�init���̳��⣩");
		System.out.println("    req rname num     ������Դ��������Ϊ��ǰ����������Ӧ��Դ");
		System.out.println("    rel rname num     ������Դ��������Ϊ��ǰ�����ͷ���Ӧ��Դ");
		System.out.println("    to                ʱ��Ƭ����");
		System.out.println("    lp                ��ӡ���н�����Ϣ");
		System.out.println("    lp -p pname       ���ݽ�������ӡ�ض�������Ϣ");
		System.out.println("    lr                ��ӡ������Դ����Ϣ");
		System.out.println("    exit              �˳�");
	}

}
