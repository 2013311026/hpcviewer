package edu.rice.cs.hpc.common.util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;


/**
 * 
 * class to manage procedure aliases
 *
 */
public class ProcedureAliasMap extends AliasMap<String,String> {

	static private final String FILE_NAME = "procedure.map";

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.util.IUserData#getFilename()
	 */
	public String getFilename() {
		
		IPath path = Platform.getLocation().makeAbsolute();
		return path.append(FILE_NAME).makeAbsolute().toString();
	}

	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.util.IUserData#initDefault()
	 */
	public void initDefault() {
		data.put("hpcrun_special_IDLE", "... IDLE ...");
	}
}
