package comp2010.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.org.apache.bcel.internal.classfile.Code;
import com.sun.org.apache.bcel.internal.classfile.Constant;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.ArithmeticInstruction;
import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.ConstantPushInstruction;
import com.sun.org.apache.bcel.internal.generic.DADD;
import com.sun.org.apache.bcel.internal.generic.FADD;
import com.sun.org.apache.bcel.internal.generic.IADD;
import com.sun.org.apache.bcel.internal.generic.ICONST;
import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.bcel.internal.generic.InstructionConstants;
import com.sun.org.apache.bcel.internal.generic.InstructionFactory;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.InstructionTargeter;
import com.sun.org.apache.bcel.internal.generic.LADD;
import com.sun.org.apache.bcel.internal.generic.LDC;
import com.sun.org.apache.bcel.internal.generic.MethodGen;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import com.sun.org.apache.bcel.internal.generic.TargetLostException;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void optimizeConstants(ClassGen cgen, ConstantPoolGen cpgen, InstructionList instList) {
		InstructionFinder ifinder = new InstructionFinder(instList);
		String constant = "(ConstantPushInstruction|LDC) ";
		String pattern = constant + constant + "ArithmeticInstruction";
		for (Iterator<InstructionHandle[]> iter = ifinder.search(pattern); iter.hasNext();) {
			InstructionHandle[] instrs = iter.next();
	
			Number val0 = getValueFromConstantInstruction(instrs[0], cpgen);
			Number val1 = getValueFromConstantInstruction(instrs[1], cpgen);
			Number result = foldConstants(val0, val1, (ArithmeticInstruction) instrs[2].getInstruction());
			
			//Create Instruction list
			InstructionFactory f  = new InstructionFactory(cgen);
			InstructionList    newIL = new InstructionList();
			
			newIL.append(f.createConstant(result));
			/*System.out.println(val0);
			System.out.println(val1);
			System.out.println(result);*/
			//Instruction new instruction before first instruction
			System.out.println("Before optimization:");
			System.out.println(instList);

			instList.insert(instrs[2].getNext(), newIL);
		
			System.out.println("Added Instructions:");
			System.out.println(instList);
			//Delete Instructions from first to last
			try {
				instList.delete(instrs[0], instrs[2]);
			} 
			catch(TargetLostException e) {
				InstructionHandle[] targets = e.getTargets();
				for(int i=0; i < targets.length; i++) {
					InstructionTargeter[] targeters = (InstructionTargeter[]) targets[i].getTargeters();
					for(int j=0; j < targeters.length; j++)
						targeters[j].updateTarget(targets[i], instrs[2].getNext());
				}
			}
			System.out.println("Result should be:");
			System.out.println(instList);
		}
	}
	
	private void optimizeMethod(ClassGen cgen, ConstantPoolGen cpgen, Method method)
	{
		/* TODO: include a boolean flag if anything was changed. If it was,
		 * recursively run the method again until nothing changes.
		 */
		// Get the Code of the method, which is a collection of bytecode instructions
		Code methodCode = method.getCode();

		// Now get the actualy bytecode data in byte array, 
		// and use it to initialise an InstructionList
		InstructionList instList = new InstructionList(methodCode.getCode());
			
		optimizeConstants(cgen, cpgen, instList);
		optimizeDynamicVariables(cgen, cpgen, instList);
		

		// Initialise a method generator with the original method as the baseline	
		MethodGen methodGen = new MethodGen(method.getAccessFlags(), method.getReturnType(),
											method.getArgumentTypes(), null, method.getName(),
											cgen.getClassName(), instList, cpgen);


		// setPositions(true) checks whether jump handles 
		// are all within the current method
		instList.setPositions(true);

		// set max stack/local
		methodGen.setMaxStack();
		methodGen.setMaxLocals();

		// generate the new method with replaced iconst
		Method newMethod = methodGen.getMethod();
		// replace the method in the original class
		cgen.replaceMethod(method, newMethod);

	}


	private void optimizeDynamicVariables(ClassGen cgen, ConstantPoolGen cpgen, InstructionList instList) {
		String searchPattern = "NOP";  // TODO: find suitable pattern.
		/* This should recognize:
		 * Variable stores when directly preceded by a constant push onto stack
		 * Variable loads
		 */
		InstructionFinder ifinder = new InstructionFinder(instList);
		HashMap<Integer, Object> valueTable = new HashMap<Integer, Object>();
		for (@SuppressWarnings("unchecked")
		Iterator<InstructionHandle[]> iter = ifinder.search(searchPattern); iter.hasNext();) {
			InstructionHandle[] instrs = iter.next();
			if (isVariableStore(instrs)) {
				updateValueTable(instList, instrs, valueTable);
			} else {  // If it's a variable load
				getValueFromTable(instList, instrs, valueTable);
			}
		}
		
	}

	private void getValueFromTable(InstructionList instList,
			InstructionHandle[] instrs, HashMap<Integer, Object> valueTable) {
		/* TODO: this method should replace the variable load with a constant
		 * push when the value is known. Note that despite any number of passes
		 * sometimes we won't be able to know the value (eg if it requires user
		 * input). In these cases we should leave the instList alone.
		 */
		
		
	}

	private void updateValueTable(InstructionList instList,
			InstructionHandle[] instrs, HashMap<Integer, Object> valueTable) {
		/*
		 * TODO: this method is called when a constant is assigned to a variable.
		 * This may be after a few passes of the optimizer. In this case we should
		 * delete the constant push and the store instruction and save the value
		 * in the valueTable for future use.
		 */
	}

	private boolean isVariableStore(InstructionHandle[] instrs) {
		// TODO Checks whether the given instructions represent a constant push
		// followed by a local variable store.
		return false;
	}

	private Number foldConstants(Number val0, Number val1, ArithmeticInstruction op) {
		if (op instanceof IADD) {
			return val0.intValue() + val1.intValue();
		} else if (op instanceof FADD) {
			return val0.floatValue() + val1.floatValue();
		} else if (op instanceof DADD) {
			return val0.doubleValue() + val1.doubleValue();
		} else if (op instanceof LADD) {
			return val0.longValue() + val1.doubleValue();
		}
		return null;
		// TODO: Add more instructions (eventually all the ones that Fu mentioned)

	}


	private Number getValueFromConstantInstruction(InstructionHandle ih, ConstantPoolGen cpgen) {
		if (ih instanceof ConstantPushInstruction) {
			return ((ConstantPushInstruction)(ih.getInstruction())).getValue();
		}  else {
			return (Number) ((LDC)(ih.getInstruction())).getValue(cpgen);
		}
		//TODO: Make sure this works (?)
	}


	public void optimize()
	{
		// load the original class into a class generator
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Do your optimization here
		Method[] methods = cgen.getMethods();
		for (Method m : methods) {
			optimizeMethod(cgen, cpgen, m);
		}
		
		
		for (Method m : cgen.getMethods()) {
			System.out.println("After optimization:");
			System.out.println(new InstructionList(m.getCode().getCode()));
		}

		// we generate a new class with modifications
		// and store it in a member variable
		this.optimized = cgen.getJavaClass();
	}

	public void write(String optimisedFilePath)
	{
		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
