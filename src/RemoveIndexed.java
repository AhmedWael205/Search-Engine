public class RemoveIndexed {

	
	private MongoDBAdapter DBAdapeter;

	public RemoveIndexed(MongoDBAdapter DBAdapeter) {
		this.DBAdapeter = DBAdapeter;
//		this.DBAdapeter.removeAllIndexedTag();
	}
	
	
	public static void main( String args[] ) {
		
		boolean Global = false;
		boolean DropTable = false;
		
		MongoDBAdapter DBAdapeter = new MongoDBAdapter(Global);
		DBAdapeter.init(DropTable);
		new RemoveIndexed(DBAdapeter);
	}
}

