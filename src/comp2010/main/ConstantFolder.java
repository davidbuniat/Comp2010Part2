package comp2010.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import com.sun.corba.se.spi.ior.IOR;
import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.org.apache.bcel.internal.classfile.Code;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.ArithmeticInstruction;
import com.sun.org.apache.bcel.internal.generic.BIPUSH;
import com.sun.org.apache.bcel.internal.generic.ClassGen;
import com.sun.org.apache.bcel.internal.generic.ConstantPoolGen;
import com.sun.org.apache.bcel.internal.generic.ConstantPushInstruction;
import com.sun.org.apache.bcel.internal.generic.DADD;
import com.sun.org.apache.bcel.internal.generic.DDIV;
import com.sun.org.apache.bcel.internal.generic.DMUL;
import com.sun.org.apache.bcel.internal.generic.DNEG;
import com.sun.org.apache.bcel.internal.generic.DREM;
import com.sun.org.apache.bcel.internal.generic.DSUB;
import com.sun.org.apache.bcel.internal.generic.FADD;
import com.sun.org.apache.bcel.internal.generic.FDIV;
import com.sun.org.apache.bcel.internal.generic.FMUL;
import com.sun.org.apache.bcel.internal.generic.FNEG;
import com.sun.org.apache.bcel.internal.generic.FREM;
import com.sun.org.apache.bcel.internal.generic.FSUB;
import com.sun.org.apache.bcel.internal.generic.IADD;
import com.sun.org.apache.bcel.internal.generic.IAND;
import com.sun.org.apache.bcel.internal.generic.IDIV;
import com.sun.org.apache.bcel.internal.generic.IMUL;
import com.sun.org.apache.bcel.internal.generic.INEG;
import com.sun.org.apache.bcel.internal.generic.IREM;
import com.sun.org.apache.bcel.internal.generic.ISHL;
import com.sun.org.apache.bcel.internal.generic.ISHR;
import com.sun.org.apache.bcel.internal.generic.ISUB;
import com.sun.org.apache.bcel.internal.generic.IUSHR;
import com.sun.org.apache.bcel.internal.generic.IXOR;
import com.sun.org.apache.bcel.internal.generic.Instruction;
import com.sun.org.apache.bcel.internal.generic.InstructionFactory;
import com.sun.org.apache.bcel.internal.generic.InstructionHandle;
import com.sun.org.apache.bcel.internal.generic.InstructionList;
import com.sun.org.apache.bcel.internal.generic.InstructionTargeter;
import com.sun.org.apache.bcel.internal.generic.LADD;
import com.sun.org.apache.bcel.internal.generic.LAND;
import com.sun.org.apache.bcel.internal.generic.LDC;
import com.sun.org.apache.bcel.internal.generic.LDIV;
import com.sun.org.apache.bcel.internal.generic.LMUL;
import com.sun.org.apache.bcel.internal.generic.LNEG;
import com.sun.org.apache.bcel.internal.generic.LOR;
import com.sun.org.apache.bcel.internal.generic.LREM;
import com.sun.org.apache.bcel.internal.generic.LSHL;
import com.sun.org.apache.bcel.internal.generic.LSHR;
import com.sun.org.apache.bcel.internal.generic.LSUB;
import com.sun.org.apache.bcel.internal.generic.LUSHR;
import com.sun.org.apache.bcel.internal.generic.LXOR;
import com.sun.org.apache.bcel.internal.generic.LoadInstruction;
import com.sun.org.apache.bcel.internal.generic.MethodGen;
import com.sun.org.apache.bcel.internal.generic.PushInstruction;
import com.sun.org.apache.bcel.internal.generic.StoreInstruction;
import com.sun.org.apache.bcel.internal.generic.TargetLostException;
import com.sun.org.apache.bcel.internal.util.InstructionFinder;

public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;
	
	HashMap<Integer, Object> valueTable = new HashMap<Integer, Object>();

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
	private boolean optimizeConstants(ClassGen cgen, ConstantPoolGen cpgen, InstructionList instList) {

		InstructionFinder ifinder = new InstructionFinder(instList);
		String constant = "(ConstantPushInstruction|LDC) ";
		String negation = "(INEG|FNEG|DNEG|LNEG)";
		String negationPattern = constant + negation;
		String binaryPattern = constant + constant + "ArithmeticInstruction";
		String pattern = "(" + negationPattern + "|" + binaryPattern + ")";
		boolean madeChanges = false;
		for (Iterator<InstructionHandle[]> iter = ifinder.search(pattern); iter.hasNext();) {
			InstructionHandle[] instrs = iter.next();

			Number result = null;

			if (instrs.length == 3) {
				Number val0 = getValueFromConstantInstruction(instrs[0], cpgen);
				Number val1 = getValueFromConstantInstruction(instrs[1], cpgen);
				result = foldConstants(val0, val1, (ArithmeticInstruction) instrs[2].getInstruction());
			} else {
				Number val = getValueFromConstantInstruction(instrs[0], cpgen);
				result = negateInstr(val, instrs[1].getInstruction());
			}
			System.out.println("result: "+ result);
			//Create Instruction list
			InstructionFactory f  = new InstructionFactory(cgen);
			InstructionList    newIL = new InstructionList();

			newIL.append(f.createConstant(result));
			/*System.out.println(val0);
			System.out.println(val1);
			System.out.println(result);*/
			//Instruction new instruction before first instruction
			//System.out.println("Before optimization:");
			//System.out.println(instList);

			instList.insert(instrs[0], newIL);

			//System.out.println("Added Instructions:");
			//System.out.println(instList);
			//Delete Instructions from first to last
			madeChanges = true;
			try {
				instList.delete(instrs[0], instrs[instrs.length - 1]);
			} 
			catch(TargetLostException e) {
				InstructionHandle[] targets = e.getTargets();
				for(int i=0; i < targets.length; i++) {
					InstructionTargeter[] targeters = (InstructionTargeter[]) targets[i].getTargeters();
					for(int j=0; j < targeters.length; j++)
						targeters[j].updateTarget(targets[i], instrs[instrs.length - 1].getNext());
				}
			}
			//System.out.println("Result should be:");
			//System.out.println(instList);
		}
		
		return madeChanges;
	}

	private Number negateInstr(Number val, Instruction instruction) {
		if (instruction instanceof INEG) {
			return val.intValue() * -1;
		} else if (instruction instanceof DNEG) {
			return val.doubleValue() * -1.0;
		} else if (instruction instanceof FNEG) {
			return val.floatValue() * -1.0f;
		} else if (instruction instanceof LNEG) {
			return val.longValue() * -1L;
		} else {
			return null;
		}
	}

	private void optimizeMethod(ClassGen cgen, ConstantPoolGen cpgen, Method method)
	{
		System.out.println("...optimizeMethod...");
		/* DONE: include a boolean flag if anything was changed. If it was,
		 * recursively run the method again until nothing changes.
		 */
		// Get the Code of the method, which is a collection of bytecode instructions
		Code methodCode = method.getCode();

		// Now get the actualy bytecode data in byte array, 
		// and use it to initialise an InstructionList
		InstructionList instList = new InstructionList(methodCode.getCode());

		boolean c = optimizeConstants(cgen, cpgen, instList);
		boolean v = optimizeDynamicVariables(cgen, cpgen, instList);
		boolean changesMade = c || v;


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
		
		if (changesMade) {
			optimizeMethod(cgen, cpgen, newMethod);
		}

	}


	private boolean optimizeDynamicVariables(ClassGen cgen, ConstantPoolGen cpgen, InstructionList instList) {
		String searchPattern = ""
				+ "((PushInstruction)(StoreInstruction))"
				+ "|"
				+ "(LoadInstruction)"; 
		// DONE: find suitable pattern.
		/* This should recognize:
		 * Variable stores when directly preceded by a constant push onto stack
		 * Variable loads
		 */
		boolean madeChange = false;
		InstructionFinder ifinder = new InstructionFinder(instList);
		for (@SuppressWarnings("unchecked")
		Iterator<InstructionHandle[]> iter = ifinder.search(searchPattern); iter.hasNext();) {
			InstructionHandle[] instrs = iter.next();
			//System.out.println("Found patter"+instrs.toString());
			if (isVariableStore(instrs)) {
				boolean b = updateValueTable(instList, instrs, valueTable, cgen, cpgen);
				madeChange |= b;
			} else {  // If it's a variable load
				boolean b = getValueFromTable(instList, instrs, valueTable, cgen);
				madeChange |= b;
			}
		}
		
		return madeChange;

	}

	private boolean getValueFromTable(InstructionList instList,
			InstructionHandle[] instrs, HashMap<Integer, Object> valueTable, ClassGen cgen) {

		/* DONE: this method should replace the variable load with a constant
		 * push when the value is known. Note that despite any number of passes
		 * sometimes we won't be able to know the value (eg if it requires user
		 * input). In these cases we should leave the instList alone.
		 */
		LoadInstruction Load = (LoadInstruction) instrs[0].getInstruction();
		int instrId = Load.getIndex();

		if(valueTable.containsKey(instrId)){

			//Create Instruction list
			InstructionFactory f  = new InstructionFactory(cgen);
			InstructionList    newIL = new InstructionList();
			
			System.out.println("getTable: " + valueTable.get(instrId));
			newIL.append(f.createConstant(valueTable.get(instrId)));
			instList.insert(instrs[0], newIL);

			//Delete Load Instructions 
			try {
				instList.delete(instrs[0]);
			} 
			catch(TargetLostException e) {
				InstructionHandle[] targets = e.getTargets();
				for(int i=0; i < targets.length; i++) {
					InstructionTargeter[] targeters = (InstructionTargeter[]) targets[i].getTargeters();
					for(int j=0; j < targeters.length; j++)
						targeters[j].updateTarget(targets[i], instrs[0].getNext());
				}
			}
			return true;
		}
		return false;
	}

	private boolean updateValueTable(InstructionList instList,
			InstructionHandle[] instrs, HashMap<Integer, Object> valueTable,ClassGen cgen, ConstantPoolGen cpgen) {

		/*
		 * TODO: this method is called when a constant is assigned to a variable.
		 * This may be after a few passes of the optimizer. In this case we should
		 * delete the constant push and the store instruction and save the value
		 * in the valueTable for future use.
		 */
		PushInstruction pushInst = (PushInstruction) instrs[0].getInstruction();
		StoreInstruction storeInst = (StoreInstruction) instrs[1].getInstruction();

		int instrId = storeInst.getIndex();

		if(pushInst instanceof ConstantPushInstruction) {
			System.out.println("pushTable: "+instrId + " " + ((ConstantPushInstruction) pushInst).getValue());
			//System.out.println();
			valueTable.put(instrId, ((ConstantPushInstruction) pushInst).getValue());
			
			try {
				instList.delete(instrs[0],instrs[1]);
				return true;
			} 
			catch(TargetLostException e) {
				InstructionHandle[] targets = e.getTargets();
				for(int i=0; i < targets.length; i++) {
					InstructionTargeter[] targeters = (InstructionTargeter[]) targets[i].getTargeters();
					for(int j=0; j < targeters.length; j++)
						targeters[j].updateTarget(targets[i], instrs[1].getNext());
				}
				return false;
			}
		}

		return false;

		//Delete Load Instructions 
	}

	private boolean isVariableStore(InstructionHandle[] instrs) {
		// DONE Checks whether the given instructions represent a constant push
		// followed by a local variable store.
		// FIXME implement extra Safety

		return (instrs.length == 2);
	}

	private Number foldConstants(Number val0, Number val1, ArithmeticInstruction op) {
		// Addition
		if (op instanceof IADD) {
			return val0.intValue() + val1.intValue();
		} else if (op instanceof FADD) {
			return val0.floatValue() + val1.floatValue();
		} else if (op instanceof DADD) {
			return val0.doubleValue() + val1.doubleValue();
		} else if (op instanceof LADD) {
			return val0.longValue() + val1.longValue();
		}
		
		// Division
		else if (op instanceof IDIV) {
			return val0.intValue() / val1.intValue();
		} else if (op instanceof FDIV) {
			return val0.floatValue() / val1.floatValue();
		} else if (op instanceof DDIV) {
			return val0.doubleValue() / val1.doubleValue();
		} else if (op instanceof LDIV) {
			return val0.longValue() / val1.doubleValue();
		}
		
		// Multiplication
		else if (op instanceof IMUL) {
			return val0.intValue() * val1.intValue();
		} else if (op instanceof FMUL) {
			return val0.floatValue() * val1.floatValue();
		} else if (op instanceof DMUL) {
			return val0.doubleValue() * val1.doubleValue();
		} else if (op instanceof LMUL) {
			return val0.longValue() * val1.doubleValue();
		}
		
		// Reminder
		else if (op instanceof IREM) {
			return val0.intValue() % val1.intValue();
		} else if (op instanceof FREM) {
			return val0.floatValue() % val1.floatValue();
		} else if (op instanceof DREM) {
			return val0.doubleValue() % val1.doubleValue();
		} else if (op instanceof LREM) {
			return val0.longValue() % val1.doubleValue();
		}
		
		// Substraction
		else if (op instanceof ISUB) {
			return val0.intValue() - val1.intValue();
		} else if (op instanceof FSUB) {
			return val0.floatValue() - val1.floatValue();
		} else if (op instanceof DSUB) {
			return val0.doubleValue() - val1.doubleValue();
		} else if (op instanceof LSUB) {
			return val0.longValue() - val1.doubleValue();
		}
		
		// Bitwise OR
		else if (op instanceof IOR) {
			return val0.intValue() | val1.intValue();
		} else if (op instanceof LOR) {
			return val0.longValue() | val1.longValue();
		}
		
		// Bitwise XOR
		else if (op instanceof IXOR) {
			return val0.intValue() ^ val1.intValue();
		} else if (op instanceof LXOR) {
			return val0.longValue() ^ val1.longValue();
		}
		
		//Bitwise AND
		else if (op instanceof IAND) {
			return val0.intValue() & val1.intValue();
		} else if (op instanceof LAND) {
			return val0.longValue() & val1.longValue();
		}
		
		//Arithmetic Shift Left
		else if (op instanceof ISHL) {
			return val0.intValue() << val1.intValue();
		} else if (op instanceof LSHL) {
			return val0.longValue() << val1.longValue();
		}
		
		//Arithmetic Shift Right
		else if (op instanceof ISHR) {
			return val0.intValue() >> val1.intValue();
		} else if (op instanceof LSHR) {
			return val0.longValue() >>> val1.longValue();
		}
		
		//Logical Shift Right
		else if (op instanceof IUSHR) {
			return val0.intValue() >>> val1.intValue();
		} else if (op instanceof LUSHR) {
			return val0.longValue() >>> val1.longValue();
		}

		return null;
	}


	private Number getValueFromConstantInstruction(InstructionHandle ih, ConstantPoolGen cpgen) {
		
		if (ih.getInstruction() instanceof ConstantPushInstruction) {
			System.out.println("push: " + ((ConstantPushInstruction)(ih.getInstruction())).getValue());
			return ((ConstantPushInstruction)(ih.getInstruction())).getValue();
		}
		else if(ih.getInstruction() instanceof LDC){
			System.out.println("load: " + (Number) ((LDC)(ih.getInstruction())).getValue(cpgen));
			return (Number) ((LDC)(ih.getInstruction())).getValue(cpgen);
		}
		return null;

	}


	public void optimize()
	{
		// load the original class into a class generator
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Do your optimization here
		Method[] methods = cgen.getMethods();
		for (Method m : methods) {
			
			valueTable.clear();
			System.out.println("~ "+cgen.getClassName()+" "+m.getName()+" ~");
			System.out.println("Before optimization:");
			System.out.println(new InstructionList(m.getCode().getCode()));
			
			System.out.println(" ");
			System.out.println("Processing optimizations:");
			optimizeMethod(cgen, cpgen, m);
			
			
		}
		for (Method m : methods) {
			System.out.println(" ");
			System.out.println("~ "+cgen.getClassName()+" "+m.getName()+" ~");
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
