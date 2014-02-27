package info.guardianproject.securereaderinterface;

import android.os.Handler;

public abstract class ThreadedTask<Params,Progress,Result> {

	private Handler mHandler;
	private Thread mThread;

	public ThreadedTask()
	{
		mHandler = new Handler();
	}
	
	protected abstract Result doInBackground(Params... values);

	protected void onPostExecute(Result result)
	{
	
	}
	
	public final ThreadedTask<Params, Progress, Result> execute (Params... params)
	{
		mThread = new Thread(new Runnable()
		{
			private Params[] mParams;
			
			public Runnable init(Params... params)
			{
				mParams = params;
				return this;
			}
			
			@Override
			public void run() 
			{
				final Result r = doInBackground(mParams);
				if (mThread != null && !mThread.isInterrupted())
				{
					mHandler.post(new Runnable()
					{
						@Override
						public void run() 
						{
							onPostExecute(r);
						}
					});
				}
			}
		}.init(params));
		mThread.start();
		return this;
	}
	
	public void cancel(boolean mayInterruptIfRunning)
	{
		// TODO: heed the mayInterruptIfRunning param.
		if (mThread != null)
			mThread.interrupt();
		mThread = null;
	}
}
