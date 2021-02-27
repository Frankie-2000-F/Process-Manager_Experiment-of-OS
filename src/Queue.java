import java.util.Deque;
import java.util.LinkedList;

public class Queue {
	private Deque<Process>[] deques;// ��ͬ���ȼ���������������� 
	private static final Queue readyQueue = new Queue();
	
	@SuppressWarnings("unchecked")
	private Queue() {
		deques = new LinkedList[3];
		for(int i = 0; i < 3; i++) {//3�ֲ�ͬ���ȼ�������3����Ӧ�������� 
			deques[i] = new LinkedList<>();
		}
	}
	
	public static Queue getReadyQueue() {
		return readyQueue;
	}
	
	// �����̼��뵽���Ӧ���ȼ��ľ���������
	public void addProcess(Process process) {
		int priority = process.getPriority();
		Deque<Process> deque = deques[priority];
		deque.addLast(process);
	}
	
	// ��þ��������������ȼ���ߵĽ��̡�������Ϊ�գ��򷵻�null
	public Process getProcess() {
		for(int i = 2; i >= 0; i--) {// �����ȼ��Ӹߵ��ͱ���
			Deque<Process> deque = deques[i];
			if(!deque.isEmpty())
				return deque.peekFirst();// �����в���ʱ���ض����е�һ������
		}
		return null;
	}
	
	// ɾ��������������ָ���Ľ��̡�ɾ���ɹ�����true�������̲����ھͷ���false��
	public boolean removeProcess(Process process) {
		int priority = process.getPriority();
		Deque<Process> deque = deques[priority];
		return deque.remove(process);
	}

}
