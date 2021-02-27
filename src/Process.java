import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Process {
	private int PID;// ����ID 
	private String processName;// ������
	private int priority;// �������ȼ� 
	private State state;// ����״̬  
	private ConcurrentHashMap<Resource, Integer> resourceMap;// ���̳��е���Դ����Ӧ���� 
	private Resource blockResource;// �������״̬Ϊ�����Ļ���������Ծ�ָ����������Դ������Ӧ��Ϊnull 
	
	private Process parent;// ���̵ĸ�����
	private List<Process> children;// ���̵��ӽ��� 
	
	private static final PCBUtil pcb = PCBUtil.getPCB();
	private static final Queue readyQueue = Queue.getReadyQueue();
	
	// ���̵���״̬��NEW���½���, READY��������,RUNNING�����У�, BLOCKED��������, TERMINATED����ֹ��
	public enum State{
		NEW, READY, RUNNING, BLOCKED, TERMINATED
	}
	
	public Process(int PID, String processName, int priority, State state, ConcurrentHashMap<Resource, Integer> resourceMap, Process parent, List<Process> children) {
		this.PID = PID;
		this.processName = processName;
		this.priority = priority;
		this.state = state;
		this.resourceMap = resourceMap;
		this.parent = parent;
		this.children = children;
	}
	
	public int getPID() {
		return PID;
	}
	
	public String getProcessName() {
		return processName;
	}
	
	public int getPriority() {
		return priority;
	}
	
	public State getState() {
		return state;
	}
	
	public void setState(State state) {
		this.state = state;
	}
	
	public void setParent(Process parent) {
		this.parent = parent;
	}
	
	public List<Process> getChildren(){
		return children;
	}
	
	public Resource getBlockResource() {
		return blockResource;
	}
	
	public Map<Resource, Integer> getResourceMap(){
		return resourceMap;
	}
	
	public void setBlockResource(Resource blockResource) {
		this.blockResource = blockResource;
	}
	
	// ɾ������ 
	public void destroy() {
		killSubTree();
		PCBUtil.scheduler();
		return;
	}
	
	// ɾ���ӽ��� 
	public void removeChild(Process process) {
		for(Process child : children) {
			if(child == process) {
				children.remove(child);
				return;
			}
		}
	}
	
	// ɾ�����̲��ݹ�ɾ�����ӽ��� 
	public void killSubTree() {
		if(!children.isEmpty()) {//��ǰ���̴����ӽ���
			int childNum = children.size();
			for(int i = 0; i < childNum; i++) {
				Process child = children.get(0);
				child.killSubTree();//�ݹ�ɾ������
			}
		}
		
		if(this.getState() == State.TERMINATED) {//��ֹ״̬��ɾ���ɹ�
			pcb.killProcess(this);
			return;
		}else if(this.getState() == State.READY) {//����״̬����ɾ��֮���޸�״̬Ϊ��ֹ
			readyQueue.removeProcess(this);
			pcb.killProcess(this);
			this.setState(State.TERMINATED);
		}else if(this.getState() == State.BLOCKED) {//����״̬����ɾ��֮���޸�״̬Ϊ��ֹ
			Resource blockResource = this.getBlockResource();
			blockResource.removeBlockProcess(this);
			pcb.killProcess(this);
			this.setState(State.TERMINATED);
		}else if(this.getState() == State.RUNNING) {//����״̬����ɾ��֮���޸�״̬Ϊ��ֹ
			pcb.killProcess(this);
			this.setState(State.TERMINATED);
		}
		
		// ������̵�parent��childָ��  
		parent.removeChild(this);
		parent = null;
		
		//�ͷ���Դ
		for(Resource resource : resourceMap.keySet()) {
			resource.release(this);
		}
		return;
	}
}
