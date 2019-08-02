package pdlab.minecraft.teamservice.main;

import java.util.List;

import pdlab.minecraft.core.PDCore;
import pdlab.minecraft.services.ServiceBase;
import pdlab.minecraft.services.ServiceInfo;

public class TeamService extends ServiceBase {
	private static final String COMMAND_LABEL = "팀";
	private CommandExecutor cmd;
	
	@Override
	public void onCreate(ServiceInfo info, PDCore core) {
		super.onCreate(info, core);
		cmd = new CommandExecutor(COMMAND_LABEL);
		core.setCommand(COMMAND_LABEL, cmd);
	}


	@Override
	public List<String> getCommandHelp() {
		return cmd.getCommandHelp();
	}
	
}
