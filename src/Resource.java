import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

public class Resource {
	private int RID;//资源ID 
	private int max;//分配的资源最大数量  
	private int remaining;//剩余的资源数量  
	private Deque<BlockProcess> blockDeque;//在该资源上阻塞的进程队列
	
	private static final PCBUtil pcb = PCBUtil.getPCB();
	private static final Queue readyQueue = Queue.getReadyQueue();
	
	// 阻塞进程类
	class BlockProcess{
		private Process process;
		private int need;//需要请求的资源数量
		
		public BlockProcess(Process process, int need) {
			this.process = process;
			this.need = need;
		}
		
		public Process getProcess() {
			return process;
		}
		
		public int getNeed() {
			return need;
		}
	}
	
	public Resource(int RID, int max) {
		this.RID = RID;
		this.max = max;
		this.remaining = max;
		blockDeque = new LinkedList<>();
	}
	
	public int getRID() {
		return RID;
	}
	
	public void addRemaining(int num) {
		this.remaining += num;
	}
	
	public boolean removeBlockProcess(Process process) {
		for(BlockProcess bProcess : blockDeque) {
			if(bProcess.getProcess() == process) {
				blockDeque.remove(bProcess);
				return true;
			}
		}
		return false;
	}
	
	 // 进程请求资源
	public void request(Process process, int need) {
		if(need > max) {// 请求数量大于最大数量时申请失败
			System.out.println("请求失败，请求资源数量超出最大值!");
			return;
		}else if(need > remaining && !"init".equals(process.getProcessName())) {// 对于非init进程需要阻塞  
			blockDeque.addLast(new BlockProcess(process, need));// 加入阻塞队列
			process.setState(Process.State.BLOCKED);// 设置进程为阻塞状态
			process.setBlockResource(this);
			PCBUtil.scheduler();//执行调度
			//System.out.println("申请资源失败，进程阻塞");
			return;
		}else if(need > remaining && "init".equals(process.getProcessName())) {//init进程不阻塞 
			return;
		}else {// 分配资源 
			remaining -= need;// 更新剩余资源数量
			Map<Resource, Integer> resourceMap = process.getResourceMap();
			if(resourceMap.containsKey(this)) {
				Integer alreadyNum = resourceMap.get(this);
				resourceMap.put(this, alreadyNum + need);// 更新已分配资源
			}else {
				resourceMap.put(this, need);
			}
		}
	}
	
	// 进程释放资源并唤醒阻塞进程  
	public void release(Process process) {
		int num = 0;
		num = process.getResourceMap().remove(this);
		if(num == 0)
			return;
		remaining += num;// 更新剩余资源 
		while(!blockDeque.isEmpty()) {
			BlockProcess blockProcess = blockDeque.peekFirst();
			int need = blockProcess.getNeed();
			if(remaining >= need) {// 若剩余资源数量大于need，则可以唤醒阻塞队列队头的一个进程
				Process readyProcess = blockProcess.getProcess();// 从阻塞队列取出进程
				request(readyProcess, need);// 进程请求资源
				blockDeque.removeFirst();// 从阻塞队列移除该进程 
				readyQueue.addProcess(readyProcess);// 加入就绪队列
				readyProcess.setState(Process.State.READY);// 进程设为就绪状态 
				readyProcess.setBlockResource(null);// 此时该进程已没有被阻塞资源
				if(readyProcess.getPriority() > pcb.getCurrentProcess().getPriority()) {
					pcb.seize(readyProcess, pcb.getCurrentProcess());// 如果唤醒的进程优先级高于当前进程优先级则抢占
				}
			}else
				break;
		}
	}
	

	public void release(Process process, int num) {
		if(num == 0)//如果释放数量为0，不执行操作
			return;
		Map<Resource, Integer> resourceMap = process.getResourceMap();
		Integer alreadyNum = resourceMap.get(this);
		if(num > alreadyNum)//如果释放资源数量大于持有数量
			System.out.println("参数错误！释放资源数超过拥有值");
		else if(num == alreadyNum)//如果刚好释放数量等于持有数量
			resourceMap.remove(this);//从资源表中移除
		else
			resourceMap.put(this, alreadyNum - num);//更新资源表中对应资源数量
		remaining += num;
		while(!blockDeque.isEmpty()) {
			BlockProcess blockProcess = blockDeque.peekFirst();
			int need = blockProcess.getNeed();
			if(remaining >= need) {
				Process readyProcess = blockProcess.getProcess();
				request(readyProcess, need);
				blockDeque.removeFirst();
				readyQueue.addProcess(readyProcess);
				readyProcess.setState(Process.State.READY);
				readyProcess.setBlockResource(null);
				if(readyProcess.getPriority() > pcb.getCurrentProcess().getPriority()) {
					pcb.seize(readyProcess, pcb.getCurrentProcess());
				}
			}else
				break;
		}
	}
	
	public void printCurrentStatus() {
		StringBuilder sb = new StringBuilder();
		sb.append("res-")
			.append(RID)
			.append("{max=")
			.append(max)
			.append(", remaining:")
			.append(remaining)
			.append(",")
			.append("blockDeque[");
		for(BlockProcess bProcess : blockDeque) {
			sb.append(",{")
				.append(bProcess.getProcess().getProcessName())
				.append(":")
				.append(bProcess.getNeed())
				.append("}");
		}
		sb.append("]}");
		String result = sb.toString();
		System.out.println(result.replaceFirst("\\[," , "\\["));
	}
}
