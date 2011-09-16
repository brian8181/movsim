package org.movsim.simulator.vehicles.lanechanging.impl;

import java.util.List;

import org.movsim.input.model.vehicle.laneChanging.LaneChangingMobilData;
import org.movsim.simulator.Constants;
import org.movsim.simulator.vehicles.Vehicle;
import org.movsim.simulator.vehicles.VehicleContainer;
import org.movsim.simulator.vehicles.impl.VehicleContainerImpl;


public class MOBILImpl {

    private double politeness; // politeness factor

    private double threshold; // changing threshold

    private double bSafe; // maximum safe braking decel

    private double gapMin; // minimum safe (net) distance

    private double biasRight; // bias (m/s^2) to drive

    // internal: save reference values
    private double thresholdRef;

    private double biasRightRef;

    private double bSafeRef;

    private double pRef;

    private final Vehicle me;

    public MOBILImpl(final Vehicle vehicle) {
        this.me = vehicle;

        // TODO handle this case with *no* <MOBIL> xml element

    }

    public MOBILImpl(final Vehicle vehicle, LaneChangingMobilData lcMobilData) {
        this.me = vehicle;
        // TODO Auto-generated constructor stub

        bSafeRef = bSafe = lcMobilData.getSafeDeceleration();
        biasRightRef = biasRight = lcMobilData.getRightBiasAcceleration();
        gapMin = lcMobilData.getMinimumGap();
        thresholdRef = threshold = lcMobilData.getThresholdAcceleration();
        pRef = politeness = lcMobilData.getPoliteness();

    }
    
    private boolean neigborsInProcessOfLaneChanging(final Vehicle v1, final Vehicle v2, final Vehicle v3 ){
        // finite delay criterion also for neigboring vehicles 
        final boolean oldFrontVehIsLaneChanging = (v1 == null) ? false : v1.inProcessOfLaneChanging();
        final boolean newFrontVehIsLaneChanging = (v2 == null) ? false : v2.inProcessOfLaneChanging();
        final boolean newBackVehIsLaneChanging  = (v3 == null) ? false : v3.inProcessOfLaneChanging();
        return  (oldFrontVehIsLaneChanging || newFrontVehIsLaneChanging || newBackVehIsLaneChanging);
    }


    private boolean safetyCheckGaps(double gapFront, double gapBack){
        return  ((gapFront < gapMin) || (gapBack < gapMin)) ;
    }
    
    private boolean safetyCheckAcceleration(double acc){
        return acc <= -bSafe;
    }

    
    public double calcAccelerationBalanceInNewLaneSymmetric(final VehicleContainer ownLane,
            final VehicleContainer newLane) {

        double prospectiveBalance = -Double.MAX_VALUE;

        final Vehicle newFront = newLane.getLeader(me);
        final Vehicle oldFront = ownLane.getLeader(me);
        final Vehicle newBack = newLane.getFollower(me);
        
        
        // check first if other vehicles are lane-changing
        if( neigborsInProcessOfLaneChanging(oldFront, newFront, newBack) ){
            return prospectiveBalance;
        }
        
        // safety: first check distances
        final double gapFront = me.getNetDistance(newFront);
        final double gapBack = (newBack == null) ? Constants.GAP_INFINITY : newBack.getNetDistance(me);
        
        if( safetyCheckGaps(gapFront, gapBack) ){
            return prospectiveBalance;
        }

        // safety: check (MOBIL) safety constraint for new follower
        final double newBackNewAccTest = (newBack == null) ? 0 : newBack.getAccelerationModel().calcAcc(newBack, me);
        
        //newLane.addTestwise(me);  // without calling init.
        //final VehicleContainer newSituationNewBack = newLane.getEnvironment(newBack);
        final VehicleContainer newSituationNewBack = new VehicleContainerImpl(0);
        newSituationNewBack.addTestwise(newBack);
        newSituationNewBack.addTestwise(me);
        final double newBackNewAcc = (newBack == null) ? 0 : newBack.calcAccModel(newSituationNewBack, null, 1,1,1);
        // compare
        if( Math.abs(newBackNewAccTest-newBackNewAcc)> 0.0001 ){
            System.err.printf("deviation in new newBackNewAcc!!!\n");// newBackOldAccTest=%.4f, newBackOldAcc=%.4f\n", newBackOldAccTest, newBackOldAcc);
          }
        
        if( safetyCheckAcceleration(newBackNewAcc)){
            return prospectiveBalance;
        }
            
        // check now incentive criterion
        // consider three vehicles: me, oldBack, newBack

        // old situation for me
        final double meOldAcc = me.calcAccModel(ownLane, null, 1, 1, 1); //, vehContainer, vehContainerLeftLane, alphaT, alphaV0)getAccelerationModel().calcAcc(me, oldFront);
        final double meOldAccTest = me.getAccelerationModel().calcAcc(me, oldFront);        
        if(Math.abs(meOldAccTest-meOldAcc)> 0.0001){
            System.err.printf("meOldAccTest=%.4f, meOldAcc=%.4f\n", meOldAccTest, meOldAcc);
        }

        // old situation for old back 
        final Vehicle oldBack = ownLane.getFollower(me);

        final double oldBackOldAcc = (oldBack != null) ? oldBack.calcAccModel(ownLane, null, 1, 1, 1) : 0;
        final double oldBackOldAccTest = (oldBack != null) ? oldBack.getAccelerationModel().calcAcc(oldBack, me) : 0;
        if(Math.abs(oldBackOldAccTest-oldBackOldAcc)> 0.0001){
          System.err.printf("oldBackAccTest=%.4f, oldBackAcc=%.4f\n", oldBackOldAccTest, oldBackOldAcc);
        }
        
        // old situation for new back
        final double newBackOldAcc = (newBack != null) ? newBack.calcAccModel(newLane, null, 1,1,1) : 0;
        final double newBackOldAccTest = (newBack != null) ? newBack.getAccelerationModel().calcAcc(newBack, newFront) : 0;
        if(Math.abs(newBackOldAccTest-newBackOldAcc)> 0.0001){
            System.err.printf("newBackOldAccTest=%.4f, newBackOldAcc=%.4f\n", newBackOldAccTest, newBackOldAcc);
          }
       
        
        // new traffic situation: set subject virtually into new lane under consideration
        final double meNewAccTest = me.getAccelerationModel().calcAcc(me, newFront);
        
        
        final VehicleContainer newSituationMe = new VehicleContainerImpl(0); //newLane.getEnvironment(me);
        newSituationMe.addTestwise(me);
        newSituationMe.addTestwise(newFront);
        final double meNewAcc = me.calcAccModel(newSituationMe, null, 1, 1, 1);
        

        // compare
        if( Math.abs(meNewAccTest-meNewAcc)> 0.0001 ){
            System.err.printf("deviation in meNewAccTest!!!\n");// newBackOldAccTest=%.4f, newBackOldAcc=%.4f\n", newBackOldAccTest, newBackOldAcc);
          }
        
        
        final double oldBackNewAccTest = (oldBack != null) ? oldBack.getAccelerationModel().calcAcc(oldBack, oldFront) : 0;
        final VehicleContainer newSituationOldBack = new VehicleContainerImpl(0); //ownLane.getEnvironment(oldBack);
        newSituationOldBack.addTestwise(oldFront);
        newSituationOldBack.addTestwise(oldBack);
        final double oldBackNewAcc = (oldBack != null) ? oldBack.calcAccModel(newSituationOldBack, null, 1, 1, 1) : 0;
        

        // compare
        if( Math.abs(oldBackNewAccTest-oldBackNewAcc)> 0.0001 ){
            System.err.printf("deviation in oldBackNewAccTest !!\n");// newBackOldAccTest=%.4f, newBackOldAcc=%.4f\n", newBackOldAccTest, newBackOldAcc);
          }
        
        
        // MOBIL trade-off for driver and neighborhood
        final double oldBackDiffAcc = oldBackNewAcc - oldBackOldAcc;
        final double newBackDiffAcc = newBackNewAcc - newBackOldAcc;
        final double meDiffAcc = meNewAcc - meOldAcc;

        
        // MOBIL's incentive formula
        final int changeTo = newLane.getLaneIndex() - ownLane.getLaneIndex();
        final double biasSign = (changeTo == Constants.TO_LEFT) ? 1 : -1;

        prospectiveBalance = meDiffAcc + politeness * (oldBackDiffAcc + newBackDiffAcc) - threshold - biasSign
                * biasRight;
        
        return prospectiveBalance;
    }

    public double getMinimumGap() {
        return gapMin;
    }

    public double getSafeDeceleration() {
        return bSafe;
    }

//    // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//    // Asymmetric MOBIL criterion for europeanRules
//    // (consider that obstacles have no vicinity (return -2)!)
//    // calc balance:
//    // deltaMe+p*(deltaLeftHandVehicle)-threshold-bias
//    // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//
//    private double calcAccelerationBalanceInNewLaneAsymmetric(final VehicleContainer ownLane, final VehicleContainer newLane){
//    
//    double prospectiveBalance = -Double.MAX_VALUE;
//
//    final Vehicle newFront = newLane.getLeader(me);
//    final Vehicle oldFront = ownLane.getLeader(me);
//    final Vehicle newBack = newLane.getFollower(me);
//    
//    
//    // check first if other vehicles are lane-changing
//    if( neigborsInProcessOfLaneChanging(oldFront, newFront, newBack) ){
//        return prospectiveBalance;
//    }
//    
//    
//    // safety: first check distances
//    final double gapFront = me.getNetDistance(newFront);
//    final double gapBack = (newBack == null) ? Constants.GAP_INFINITY : newBack.getNetDistance(me);
//    
//    if( safetyCheckGaps(gapFront, gapBack) ){
//        return prospectiveBalance;
//    }
//    
//    // safety: check (MOBIL) safety constraint for new follower
//    final double newBackNewAcc = (newBack == null) ? 0 : newBack.getAccelerationModel().calcAcc(newBack, me);
//   
//    if( safetyCheckAcceleration(newBackNewAcc)){
//        return prospectiveBalance;
//    }
//    
    
    
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        // (3)check now incentive criterion
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

        // calculate accelerations only for vehicle TO_LEFT
        // since back vehicle cannot improve its situation in fact!
        // so choose either new or old back vehicle depending on changeTo dir
        // newBackNewAcc already calculated above!

//        int iLeftHandBack = (changeTo == SimConstants.TO_LEFT) ? me.iBackLeft() : me.iBack();
//        IMoveableExt leftHandBack = (iLeftHandBack == -1) ? null : (IMoveableExt) microstreet.vehContainer().get(iLeftHandBack);
//
//        // IMoveableExt leftHandBack = (changeTo == SimConstants.TO_LEFT) ? me.vehBackLeft() : me.vehBack();
//
//        // int iLeftHandBackFront = (leftHandBack == null) ? -1 : leftHandBack.iFront();
//        // IMoveableExt leftHandBackFront = (iLeftHandBackFront < 0) ? null
//        // : (IMoveableExt) microstreet.vehContainer().get(iLeftHandBackFront);
//        IMoveableExt leftHandBackFront = (leftHandBack == null) ? null : leftHandBack.vehFront();
//
//        // int iLeftHandBackFrontLeft = (leftHandBack == null) ? -1 : leftHandBack.iFrontLeft();
//        // IMoveableExt leftHandBackFrontLeft = (iLeftHandBackFrontLeft < 0) ? null
//        // : (IMoveableExt) microstreet.vehContainer().get(iLeftHandBackFrontLeft);
//
//        IMoveableExt leftHandBackFrontLeft = (leftHandBack == null) ? null : leftHandBack.vehFrontLeft();
//
//        double accWithMe =
//                (changeTo == SimConstants.TO_LEFT) ? newBackNewAcc : calcMobilAcceleration(changeTo, alpha_T, 1, leftHandBack,
//                        me, leftHandBackFrontLeft);
//        double accWithoutMe =
//                calcMobilAcceleration(changeTo, alpha_T, 1, leftHandBack, leftHandBackFront, leftHandBackFrontLeft);
//
//        double deltaAccLeftHandBack =
//                (changeTo == SimConstants.TO_LEFT) ? (accWithMe - accWithoutMe) : (accWithoutMe - accWithMe);
//
//        // calculate the actual and prospective acc for subject "me"
//        // actual acceleration:
//
//        // int iFront = me.iFront();
//        // IMoveableExt front = (iFront == -1) ? null : microstreet.vehContainer().get(iFront);
//        IMoveableExt front = me.vehFront();
//        // int iFrontLeft = me.iFrontLeft();
//        // IMoveableExt frontLeft = (iFrontLeft == -1) ? null : microstreet.vehContainer().get(iFrontLeft);
//
//        IMoveableExt frontLeft = me.vehFront();
//
//        double meAcc = calcMobilAcceleration(changeTo, alpha_T, 1, me, front, frontLeft);
//
//        // int iFront = (changeTo == SimConstants.TO_LEFT) ? me.iFrontLeft() : me.iFrontRight();
//        // front = (iFront == -1) ? null : microstreet.vehContainer().get(iFront);
//        front = (changeTo == SimConstants.TO_LEFT) ? me.vehFrontLeft() : me.vehFrontRight();
//
//        // treat special case when left neigbour for NEW situation after
//        // change TO_LEFT is needed for europeanAcc ...
//        // iFrontLeft = (changeTo == SimConstants.TO_LEFT) ? me.iNextFrontLeft() : me.iFront();
//        // Logger.log("LaneChange.Asymmetric: nextFrontLeft = "+iFrontLeft);
//        // frontLeft = (iFrontLeft == -1) ? null : microstreet.vehContainer().get(iFrontLeft);
//        frontLeft = (changeTo == SimConstants.TO_LEFT) ? me.vehNextFrontLeft() : me.vehFront();
//
//        // arne 24-11-04: test for change to right rescaling of new distance
//        double myVel = me.vel();
//        double alpha_sLoc = ((changeTo == SimConstants.TO_RIGHT) && (!me.isTruck()) && (myVel > vCritEur)) ? alpha_s : 1;
//
//        double meNewAcc = calcMobilAcceleration(changeTo, alpha_T, alpha_sLoc, me, front, frontLeft);
//        double deltaAccMe = meNewAcc - meAcc;
//
//        // check for mandatory change
//        if (mandatoryLaneChange(changeTo, meNewAcc, me, newBack, microstreet.mostRightLane()))
//            return (ModelConstants.BMAX);
//
//        // (ii) MOBIL trade-off for driver and neigbourhood
//        // (gleichmaessiges Auffuellen der Spur:)
//        // vehicles already on the correct lane want to change
//        // if other lane is empty
//
//        // bias sign applies for euro as symmetric rules!!!
//        // but in case of mandatory lc the sign is modified!
//        double biasSign = (changeTo == SimConstants.TO_LEFT) ? 1 : -1;
//
//        // finally: all in one MOBIL's incentive formula:
//        prospectiveBalance = deltaAccMe + p * deltaAccLeftHandBack - threshold - biasSign * biasRight;
//// if(false && changeTo==SimConstants.TO_RIGHT)Logger.log(String.format("LC: id=%d, prospectiveBalance: changeTo=%2d " +
//// "  biasSign=%3.1f biasRight=%4.1f, deltaAccMe=%4.1f, pros.Balance=%4.1f %n",me.id(), changeTo, biasSign, biasRight,
//        // deltaAccMe, prospectiveBalance));
//        return (prospectiveBalance);
//    }

}
