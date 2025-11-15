/*
 * Copyright (c) 2019 Abex
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.launcher;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Slf4j
public class SplashScreen extends JFrame implements ActionListener
{
	private static final Color BRAND_ORANGE = new Color(0, 194, 0/*220, 138, 0*/);
	private static final Color DARKER_GRAY_COLOR = new Color(30, 30, 30);

	private static final int WIDTH = 200;
	private static final int PAD = 10;

	private static SplashScreen INSTANCE;

	private final JLabel action = new JLabel("Loading");
	private final JProgressBar progress = new JProgressBar();
	private final JLabel subAction = new JLabel();
	private final Timer timer;

	private volatile double overallProgress = 0;
	private volatile String actionText = "Loading";
	private volatile String subActionText = "";
	private volatile String progressText = null;

	private SplashScreen() throws IOException
	{
		setTitle(Constants.SERVER_NAME + " Launcher");

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setUndecorated(true);
		try (var in = SplashScreen.class.getResourceAsStream(LauncherProperties.getRuneLite128()))
		{
			if (in != null)
			{
				setIconImage(ImageIO.read(in));
			}
		}
		setLayout(null);
		Container pane = getContentPane();
		pane.setBackground(DARKER_GRAY_COLOR);

		Font font = new Font(Font.DIALOG, Font.PLAIN, 12);

		BufferedImage logo = null;
		try (var in = SplashScreen.class.getResourceAsStream(LauncherProperties.getRuneLiteSplash()))
		{
			if (in != null)
			{
				logo = ImageIO.read(in);
			}
		}
		JLabel logoLabel;
		if (logo != null)
		{
			logoLabel = new JLabel(new ImageIcon(logo));
		}
		else
		{
			// Fallback to text label if image is missing
			logoLabel = new JLabel(Constants.SERVER_NAME);
			logoLabel.setForeground(Color.WHITE);
			logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
			logoLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
		}
		pane.add(logoLabel);
		logoLabel.setBounds(0, 0, WIDTH, WIDTH);

		int y = WIDTH;

		pane.add(action);
		action.setForeground(Color.WHITE);
		action.setBounds(0, y, WIDTH, 16);
		action.setHorizontalAlignment(SwingConstants.CENTER);
		action.setFont(font);
		y += action.getHeight() + PAD;

		pane.add(progress);
		progress.setForeground(BRAND_ORANGE);
		progress.setBackground(BRAND_ORANGE.darker().darker());
		progress.setBorder(new EmptyBorder(0, 0, 0, 0));
		progress.setBounds(0, y, WIDTH, 14);
		progress.setFont(font);
		progress.setUI(new BasicProgressBarUI()
		{
			@Override
			protected Color getSelectionBackground()
			{
				return Color.BLACK;
			}

			@Override
			protected Color getSelectionForeground()
			{
				return Color.BLACK;
			}
		});
		y += 12 + PAD;

		pane.add(subAction);
		subAction.setForeground(Color.LIGHT_GRAY);
		subAction.setBounds(0, y, WIDTH, 16);
		subAction.setHorizontalAlignment(SwingConstants.CENTER);
		subAction.setFont(font);
		y += subAction.getHeight() + PAD;

		setSize(WIDTH, y);
		setLocationRelativeTo(null);

		timer = new Timer(100, this);
		timer.setRepeats(true);
		timer.start();

		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		action.setText(actionText);
		subAction.setText(subActionText);
		progress.setMaximum(1000);
		progress.setValue((int) (overallProgress * 1000));

		String progressText = this.progressText;
		if (progressText == null)
		{
			progress.setStringPainted(false);
		}
		else
		{
			progress.setStringPainted(true);
			progress.setString(progressText);
		}
	}

	public static void init()
	{
		if (INSTANCE != null)
		{
			log.debug("Splash screen already initialized");
			return;
		}
		
		try
		{
			log.debug("Creating splash screen on EDT...");
			SwingUtilities.invokeAndWait(() ->
			{
				if (INSTANCE != null)
				{
					return;
				}

				try
				{
					log.debug("Setting look and feel...");
					UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
					log.debug("Creating splash screen instance...");
					INSTANCE = new SplashScreen();
					log.debug("Splash screen instance created successfully");
				}
				catch (Exception e)
				{
					log.warn("Unable to start splash screen", e);
					INSTANCE = null; // Ensure INSTANCE is null on failure
				}
			});
			log.debug("Splash screen init completed, INSTANCE is {}", INSTANCE != null ? "created" : "null");
		}
		catch (InterruptedException e)
		{
			log.warn("Splash screen init interrupted", e);
			Thread.currentThread().interrupt();
		}
		catch (InvocationTargetException e)
		{
			log.warn("Splash screen init failed", e.getCause());
		}
		catch (Exception e)
		{
			log.warn("Unexpected error initializing splash screen", e);
		}
	}

	public static void stop()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (INSTANCE == null)
			{
				return;
			}

			INSTANCE.timer.stop();
			// The CLOSE_ALL_WINDOWS quit strategy on MacOS dispatches WINDOW_CLOSING events to each frame
			// from Window.getWindows. However, getWindows uses weak refs and relies on gc to remove windows
			// from its list, causing events to get dispatched to disposed frames. The frames handle the events
			// regardless of being disposed and will run the configured close operation. Set the close operation
			// to DO_NOTHING_ON_CLOSE prior to disposing to prevent this.
			INSTANCE.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			INSTANCE.dispose();
			INSTANCE = null;
		});
	}

	public static void stage(double overallProgress, @Nullable String actionText, String subActionText)
	{
		stage(overallProgress, actionText, subActionText, null);
	}

	public static void stage(double startProgress, double endProgress,
		@Nullable String actionText, String subActionText,
		long done, long total, boolean mib)
	{
		String progress;
		if (mib)
		{
			final double MiB = 1024 * 1024;
			final double CEIL = 1.d / 10.d;
			progress = String.format("%.1f / %.1f MiB", done / MiB, (total / MiB) + CEIL);
		}
		else
		{
			progress = done + " / " + total;
		}
		stage(startProgress + ((endProgress - startProgress) * done / total), actionText, subActionText, progress);
	}

	public static void stage(double overallProgress, @Nullable String actionText, String subActionText, @Nullable String progressText)
	{
		if (INSTANCE != null)
		{
			INSTANCE.overallProgress = overallProgress;
			if (actionText != null)
			{
				INSTANCE.actionText = actionText;
			}
			INSTANCE.subActionText = subActionText;
			INSTANCE.progressText = progressText;
		}
	}
}
