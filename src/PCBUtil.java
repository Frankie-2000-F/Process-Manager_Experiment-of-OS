import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PCBUtil {
	private static final PCBUtil pcb = new PCBUtil();
	private static final Queue readyQueue = Queue.getReadyQueue();
	
	public static PCBUtil getPCB() {
		return pcb;
	}
	
	private static Map<String, Process> existProcesses;//哈希表存储现有的进程
	private Process currentProcess;//当前运行的进程
	private AtomicInteger pidGenerator;//编号自动生成器
	
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
		// 为新建进程分配PID，进程名，优先级，进程状态，资源，父进程和子进程信息等  
		Process process = new Process(pcb.generatePID(), processName, priority, Process.State.NEW, new ConcurrentHashMap<>(), currentProcess, new LinkedList<>());
		if (currentProcess != null) {// 排除创建的进程为第一个进程的特殊情况
			currentProcess.getChildren().add(process);// 新创建进程作为当前进程的子进程
			process.setParent(currentProcess);// 旧进程作为新创建进程的父进程
		}
		pcb.addExistList(process);//将进程添加至现有进程表
		readyQueue.addProcess(process);//将进程加入就绪队列
		process.setState(Process.State.READY);
		PCBUtil.scheduler();//执行调度
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
	
	//进程调度
	public static void scheduler() {
		Process currentProcess = pcb.getCurrentProcess();
		Process readyProcess = readyQueue.getProcess();//获取绪队列中的第一个进程
		if(readyProcess == null) {// 就绪队列为空时，只有init进程
			pcb.getCurrentProcess().setState(Process.State.RUNNING);// 状态设为运行状态 
		}else if(currentProcess == null) {
			readyQueue.removeProcess(readyProcess);
			pcb.setCurrentProcess(readyProcess);
			readyProcess.setState(Process.State.RUNNING);
			return;
		}else if(currentProcess.getState() == Process.State.BLOCKED || currentProcess.getState() == Process.State.TERMINATED) {//当前进程被阻塞或者已经被终止
			readyQueue.removeProcess(readyProcess);// 从就绪队列取出一个就绪进程
			pcb.setCurrentProcess(readyProcess);// 将该进程设为当前运行的进程
			readyProcess.setState(Process.State.RUNNING);// 该进程状态设为运行状态
		}else if(currentProcess.getState() == Process.State.RUNNING) {// 当前进程处于运行状态，说明执行的操作为新创建了进程，或者阻塞队列中进程转移到readyList
			if(currentProcess.getPriority() < readyProcess.getPriority()) { // 若就绪进程优先级更高，则切换进程
				seize(readyProcess, currentProcess);
			}
		}else if(currentProcess.getState() == Process.State.READY) {// 时间片完的情况
			if(currentProcess.getPriority() <= readyProcess.getPriority()) {// 若有优先级大于或等于当前进程的就绪进程，则切换进程
				seize(readyProcess, currentProcess);
			}else// 如果没有高优先级的就绪进程，则当前进程依然继续运行
				currentProcess.setState(Process.State.RUNNING);
		}
		return;
	}
	
	//进程抢占
	public static void seize(Process readyProcess, Process currentProcess) {
		if(existName(currentProcess.getProcessName())) {
			readyQueue.addProcess(currentProcess);// 将当前进程加入就绪队列中
			currentProcess.setState(Process.State.READY);// 将进程状态置为就绪状态
			readyQueue.removeProcess(readyProcess);// 从就绪队列取出一个就绪进程
			pcb.setCurrentProcess(readyProcess);// 将该进程设为当前运行的进程
			readyProcess.setState(Process.State.RUNNING);// 该进程状态设为运行状态
			return;
		}
	}
	
	// 时间片轮转（RR），时间片完后切换进程  
	public static void timeout() {
		pcb.getCurrentProcess().setState(Process.State.READY); // 时间片完直接将当前运行进程置为就绪状态 
		scheduler();//执行调度
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
		System.out.print(process.getProcessName() + "(PID:" + process.getPID() + ", 状  态: " + process.getState() + ",优先级： " + process.getPriority() + ",");
		if(process.getResourceMap().isEmpty()) {
			System.out.println("(无资源))");
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
