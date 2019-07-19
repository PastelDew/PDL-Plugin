package pdlab.minecraft.debug;

public class PDLException extends Exception {
	private static final long serialVersionUID = -2424279425932843591L;
	
	public enum ErrorInfo {
		UNKOWN(0, "Unkown error!"),
		CORE_NOT_INITIALIZED(1, "PDCore did not initialized! You have to call PDCore.initialize() or PDCore.getInstance(JavaPlugin plugin) before."),
		CORE_ALREADY_INITIALIZED(2, "PDCore was already initialized! You cannot initialize the core twice!"),
		INVALID_ARGUMENTS(3, "Invalid arguments!"),
		NOT_PERMITTED(4, "You don't have permission to do this!")
		;
		
		private final int code;
		private final String msg;
		private ErrorInfo(int code, String msg) {
			this.code = code;
			this.msg = msg;
		}
		
		public int getErrorCode() { return code; }
		public String getErrorMessage() { return msg; }
	};
	
	private ErrorInfo info;
	private String detailMessage;
	public PDLException() {
		this(ErrorInfo.UNKOWN);
	}
	
	public PDLException(String errorDetailMSG) {
		this.info = ErrorInfo.UNKOWN;
		this.detailMessage = errorDetailMSG;
	}
	
	public PDLException(ErrorInfo info) {
		super(info.getErrorMessage());
		this.info = info;
	}
	
	public int getErrorCode() {
		return info.getErrorCode();
	}
	
	public String getErrorMessage() {
		return info.getErrorMessage();
	}
	
	public String getErrorDetailMessage() {
		return detailMessage;
	}
	
	public ErrorInfo getErrorInfo() {
		return info;
	}
	
}
