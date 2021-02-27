import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PCBUtil {
	private static final PCBUtil pcb = new PCBUtil();
	private static final Queue readyQueue = Queue.getReadyQueue();
	
	public static PCBUtil getPCB() {
		return pcb;
	}
	
	private static Map<String, Process> existProcesses;//��ϣ��洢���еĽ���
	private Process currentProcess;//��ǰ���еĽ���
	private AtomicInteger pidGenerator;//����Զ�������
	
	private PCBUtil() {
		existProcesses = new HashMap<>();
		pidGenerator = new AtomicInteger();
	}
	
	public Process getCurrentProcess() {
		return currentProcess;
	}
	
	public void setCurrentProcess(Process currentProcess) {
		this.currentProcess = currentProcess;
	}
	
	public int generatePID() {
		return pidGenerator.getAndIncrement();
	}
	
	public void addExistList(Process process) {
		existProcesses.put(process.getProcessName(), process);
	}
	
	public Process createProcess(String processName, int priority) {
		Process currentProcess = pcb.getCurrentProcess();
		// Ϊ�½����̷���PID�������������ȼ�������״̬����Դ�������̺��ӽ�����Ϣ��  
		Process process = new Process(pcb.generatePID(), processName, priority, Process.State.NEW, new ConcurrentHashMap<>(), currentProcess, new LinkedList<>());
		if (currentProcess != null) {// �ų������Ľ���Ϊ��һ�����̵��������
			currentProcess.getChildren().add(process);// �´���������Ϊ��ǰ���̵��ӽ���
			process.setParent(currentProcess);// �ɽ�����Ϊ�´������̵ĸ�����
		}
		pcb.addExistList(process);//��������������н��̱�
		readyQueue.addProcess(process);//�����̼����������
		process.setState(Process.State.READY);
		PCBUtil.scheduler();//ִ�е���
		return process;	
	}
	
	public static boolean existName(String name) {
		return existProcesses.containsKey(name);
	}
	
	public Process findProcess(String processName) {
		for(Map.Entry<String, Process> entry : existProcesses.entrySet()) {
			String name = entry.getKey();
			if(processName.equals(name))
				return entry.getValue();
		}
		return null;
	}
	
	//���̵���
	public static void scheduler() {
		Process currentProcess = pcb.getCurrentProcess();
		Process readyProcess = readyQueue.getProcess();//��ȡ�������еĵ�һ������
		if(readyProcess == null) {// ��������Ϊ��ʱ��ֻ��init����
			pcb.getCurrentProcess().setState(Process.State.RUNNING);// ״̬��Ϊ����״̬ 
		}else if(currentProcess == null) {
			readyQueue.removeProcess(readyProcess);
			pcb.setCurrentProcess(readyProcess);
			readyProcess.setState(Process.State.RUNNING);
			return;
		}else if(currentProcess.getState() == Process.State.BLOCKED || currentProcess.getState() == Process.State.TERMINATED) {//��ǰ���̱����������Ѿ�����ֹ
			readyQueue.removeProcess(readyProcess);// �Ӿ�������ȡ��һ����������
			pcb.setCurrentProcess(readyProcess);// ���ý�����Ϊ��ǰ���еĽ���
			readyProcess.setState(Process.State.RUNNING);// �ý���״̬��Ϊ����״̬
		}else if(currentProcess.getState() == Process.State.RUNNING) {// ��ǰ���̴�������״̬��˵��ִ�еĲ���Ϊ�´����˽��̣��������������н���ת�Ƶ�readyList
			if(currentProcess.getPriority() < readyProcess.getPriority()) { // �������������ȼ����ߣ����л�����
				seize(readyProcess, currentProcess);
			}
		}else if(currentProcess.getState() == Process.State.READY) {// ʱ��Ƭ������
			if(currentProcess.getPriority() <= readyProcess.getPriority()) {// �������ȼ����ڻ���ڵ�ǰ���̵ľ������̣����л�����
				seize(readyProcess, currentProcess);
			}else// ���û�и����ȼ��ľ������̣���ǰ������Ȼ��������
				currentProcess.setState(Process.State.RUNNING);
		}
		return;
	}
	
	//������ռ
	public static void seize(Process readyProcess, Process currentProcess) {
		if(existName(currentProcess.getProcessName())) {
			readyQueue.addProcess(currentProcess);// ����ǰ���̼������������
			currentProcess.setState(Process.State.READY);// ������״̬��Ϊ����״̬
			readyQueue.removeProcess(readyProcess);// �Ӿ�������ȡ��һ����������
			pcb.setCurrentProcess(readyProcess);// ���ý�����Ϊ��ǰ���еĽ���
			readyProcess.setState(Process.State.RUNNING);// �ý���״̬��Ϊ����״̬
			return;
		}
	}
	
	// ʱ��Ƭ��ת��RR����ʱ��Ƭ����л�����  
	public static void timeout() {
		pcb.getCurrentProcess().setState(Process.State.READY); // ʱ��Ƭ��ֱ�ӽ���ǰ���н�����Ϊ����״̬ 
		scheduler();//ִ�е���
	}
	
	public void killProcess(Process process) {
		String name = process.getProcessName();
		existProcesses.remove(name);
	}
	
	public void printProcessTree(Process process, int retract) {
		for(int i = 0; i < retract; i++) {
			System.out.print("--");
		}
		
		printProcessDetail(process);
		List<Process> children = process.getChildren();
		for(int i = 0; i< children.size(); i++) {
			Process child = children.get(i);
			printProcessTree(child, retract + 1);
		}
	}
	
	public void printProcessDetail(Process process) {
		System.out.print(process.getProcessName() + "(PID:" + process.getPID() + ", ״  ̬: " + process.getState() + ",���ȼ��� " + process.getPriority() + ",");
		if(process.getResourceMap().isEmpty()) {
			System.out.println("(����Դ))");
		}else {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			for (Map.Entry<Resource, Integer> entry : process.getResourceMap().entrySet()) {
				Resource res = entry.getKey();
				int holdNum = entry.getValue();
				sb.append(",").append("R").append(res.getRID()).append(":").append(holdNum);
			}
			sb.append(")");
			String result = sb.toString();
			System.out.println(result.replaceFirst(",", ""));
		}
	}
	
}
