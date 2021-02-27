import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

public class Resource {
	private int RID;//��ԴID 
	private int max;//�������Դ�������  
	private int remaining;//ʣ�����Դ����  
	private Deque<BlockProcess> blockDeque;//�ڸ���Դ�������Ľ��̶���
	
	private static final PCBUtil pcb = PCBUtil.getPCB();
	private static final Queue readyQueue = Queue.getReadyQueue();
	
	// ����������
	class BlockProcess{
		private Process process;
		private int need;//��Ҫ�������Դ����
		
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
	
	 // ����������Դ
	public void request(Process process, int need) {
		if(need > max) {// �������������������ʱ����ʧ��
			System.out.println("����ʧ�ܣ�������Դ�����������ֵ!");
			return;
		}else if(need > remaining && !"init".equals(process.getProcessName())) {// ���ڷ�init������Ҫ����  
			blockDeque.addLast(new BlockProcess(process, need));// ������������
			process.setState(Process.State.BLOCKED);// ���ý���Ϊ����״̬
			process.setBlockResource(this);
			PCBUtil.scheduler();//ִ�е���
			//System.out.println("������Դʧ�ܣ���������");
			return;
		}else if(need > remaining && "init".equals(process.getProcessName())) {//init���̲����� 
			return;
		}else {// ������Դ 
			remaining -= need;// ����ʣ����Դ����
			Map<Resource, Integer> resourceMap = process.getResourceMap();
			if(resourceMap.containsKey(this)) {
				Integer alreadyNum = resourceMap.get(this);
				resourceMap.put(this, alreadyNum + need);// �����ѷ�����Դ
			}else {
				resourceMap.put(this, need);
			}
		}
	}
	
	// �����ͷ���Դ��������������  
	public void release(Process process) {
		int num = 0;
		num = process.getResourceMap().remove(this);
		if(num == 0)
			return;
		remaining += num;// ����ʣ����Դ 
		while(!blockDeque.isEmpty()) {
			BlockProcess blockProcess = blockDeque.peekFirst();
			int need = blockProcess.getNeed();
			if(remaining >= need) {// ��ʣ����Դ��������need������Ի����������ж�ͷ��һ������
				Process readyProcess = blockProcess.getProcess();// ����������ȡ������
				request(readyProcess, need);// ����������Դ
				blockDeque.removeFirst();// �����������Ƴ��ý��� 
				readyQueue.addProcess(readyProcess);// �����������
				readyProcess.setState(Process.State.READY);// ������Ϊ����״̬ 
				readyProcess.setBlockResource(null);// ��ʱ�ý�����û�б�������Դ
				if(readyProcess.getPriority() > pcb.getCurrentProcess().getPriority()) {
					pcb.seize(readyProcess, pcb.getCurrentProcess());// ������ѵĽ������ȼ����ڵ�ǰ�������ȼ�����ռ
				}
			}else
				break;
		}
	}
	

	public void release(Process process, int num) {
		if(num == 0)//����ͷ�����Ϊ0����ִ�в���
			return;
		Map<Resource, Integer> resourceMap = process.getResourceMap();
		Integer alreadyNum = resourceMap.get(this);
		if(num > alreadyNum)//����ͷ���Դ�������ڳ�������
			System.out.println("���������ͷ���Դ������ӵ��ֵ");
		else if(num == alreadyNum)//����պ��ͷ��������ڳ�������
			resourceMap.remove(this);//����Դ�����Ƴ�
		else
			resourceMap.put(this, alreadyNum - num);//������Դ���ж�Ӧ��Դ����
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
