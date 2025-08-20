# Auto Tune Module

The Auto Tune module analyses live logger data and produces recommendations for
adjusting MAF scaling or fueling tables.  It subscribes to MAF, AFR, fuel trim
and RPM parameters and compares measured values against targets defined by a
selected modification profile.

## Usage

1. Open the **Auto Tune** tab in the ECU logger.
2. Choose a modification profile from the drop-down list.
3. Click **Record** to start collecting data while the engine is running.
4. Generated recommendations appear in the right-hand panel. Use **Export** to
   save them to a file for later reference or import into the ECU editor.

## Algorithm

For each logger sample the manager computes:

- AFR deviation from the profile target.  Deviations larger than the profile's
  tolerance yield a fueling adjustment recommendation.
- Total fuel trim (`AF Correction 1` + `AF Learning 1`).  Values outside the
  profile threshold result in a MAF scaling adjustment recommendation.

### Example – Cold Air Intake

Using the `ColdAirIntake` profile (target AFR 14.7, ±0.5 tolerance, ±5% trim
threshold) a sample with AFR 15.3 and combined trim of -6% would generate:

```
AFR 15.30 dev 0.60 -> adjust fuel -4.08%
MAF 80.00 g/s adjust 6.00%
```
