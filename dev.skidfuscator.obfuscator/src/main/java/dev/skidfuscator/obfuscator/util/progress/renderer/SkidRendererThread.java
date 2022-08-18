package dev.skidfuscator.obfuscator.util.progress.renderer;

import lukfor.progress.renderer.AbstractProgressRenderer;
import lukfor.progress.renderer.IProgressRenderer;

import java.util.function.Consumer;

public class SkidRendererThread implements Runnable {

	public static float FRAME_RATE = 1 / 10f;

	private final AbstractProgressRenderer renderer;
	private final Consumer<AbstractProgressRenderer> callback;

	private long renderTime = 0;

	public SkidRendererThread(AbstractProgressRenderer renderer, Consumer<AbstractProgressRenderer> callback) {
		this.renderer = renderer;
		this.callback = callback;
	}

	@Override
	public void run() {

		while (renderer.isRunning()) {

			/*
			 * float delta = (System.currentTimeMillis() - renderTime) / 1000f;
			 * 
			 * if (delta < FRAME_RATE) { return; }
			 */

			renderer.render();

			renderTime = System.currentTimeMillis();

			try {
				Thread.sleep((int) (FRAME_RATE * 1000));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		if (callback != null) {
			callback.accept(renderer);
			renderer.render();
		}
	}

}
