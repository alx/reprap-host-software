/**
 * 
 */
package org.reprap.gui.steppertest;

/**
 * @author Ed Sells
 *
 */
public class StepperExerciser {

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{

		// Request Stepper speed
		// Request Step number
		// Request delay between strokes (for cooling)
		// Request number of runs (includes return stroke)
		
		int stepperExcerciserRepeatabilityRuns = 2;
		int stepperExcerciserRepeatabilityStepsPerStroke = 2000;
		
		for (int i = 0; i <= stepperExcerciserRepeatabilityRuns; i++)
		{
			for (int j = 1; j <= stepperExcerciserRepeatabilityStepsPerStroke; j++)
			{
				// Go out
			}
			 
			for (int k = 1; k <= stepperExcerciserRepeatabilityStepsPerStroke; k++)
			{
				// Come back
			}
			
		}
		
	}

}
