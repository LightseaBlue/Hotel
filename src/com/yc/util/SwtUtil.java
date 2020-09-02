package com.yc.util;

import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class SwtUtil {

	
	public static void showMessage(Shell shell, String message, String title) {
		MessageBox mb=new MessageBox(shell);
		mb.setMessage(message);
		mb.setText(title);
		mb.open();
		
	}
}
