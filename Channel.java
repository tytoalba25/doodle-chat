import java.util.ArrayList;

public class Channel {
	private String name;
	private ArrayList<Member> members;

	public Channel(String n) {
		name = n;
		members = new ArrayList<Member>();
	}

	public Member getMemberByIndex(int index) {
		return members.get(index);
	}
	
	// Add a member to the channel
	public int addMember(Member m) {
		for (int i = 0; i < members.size(); i++) {
			if (m.getID() == members.get(i).getID()) {
				return 0;
			}
		}
		members.add(m);
		return 1;
	}

	// Remove a member from the channel
	public int removeMember(int id) {
		for (int i = 0; i < members.size(); i++) {
			if (id == members.get(i).getID()) {
				members.remove(i);
				return 1;
			}
		}
		return 0;
	}
	
	// Return a string representing the channel
	public String toString() {
		return name;
	}

	// Returns a member matching an id
	public Member getMemberByID(int ID) {
		for (Member member : members) {
			if (member.getID() == ID)
				return member;
		}
		return null;
	}
	
	public int getPopulation() {
		return members.size();
	}
}