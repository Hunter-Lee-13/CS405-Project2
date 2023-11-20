import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
public class ContigousMemoryAllocator {
	private int size;    // maximum memory size in bytes (B)
	private int nextFitStartIndex = 0; //helps keep track of placement for next fit
	private Map<String, Partition> allocMap;   // map process to partition
	private List<Partition> partList;    // list of memory partitions
	// constructor
	public ContigousMemoryAllocator(int size) {
		this.size = size;
		this.allocMap = new HashMap<>();
		this.partList = new ArrayList<>();
		this.partList.add(new Partition(0, size)); //add the first hole, which is the whole memory at start up
	}
      
	//finds the process name 
	public Process findProcessByName(List<Process> processes, String name) {
	    for (Process process : processes) {
	        if (process.getName().equals(name)) {
	            return process;
	        }
	    }
	    return null;
	}
	
	//uses a String builder to build the string that prints the status of the memory
	public void print_status(List<Process> processes) {

		System.out.println("\n------------------------------------------------------------------------------"
				+ "-------------------------------------------------------------------------------------------"
				+ "---------------------------------------------------------------------------------------------");
		    order_partitions();
		    StringBuilder status = new StringBuilder();
		    status.append("\n|");
		    //goes through all the parts in the part list
		    for (Partition part : partList) { 
		        if (part.isbFree()) { 
		        	//if the partition is free it adds the msg to the string
		            status.append(" Free (" + part.getLength() + " KB) ");
		        } else { 
		        	//if partition is full, add following information to the string
		            Process process = findProcessByName(processes, part.getProcess());
		            status.append(" " + process.getName() + " [" +((double)process.getTime()/1000) + "s] (" + part.getLength() + " KB) ");
		        }
		    }
		    status.append("|");
		    System.out.println(status.toString());
		}
    
	//prints the number of holes, avg size of holes
	public void printMemoryStats() {
	    int totalFreeMemory = 0;
	    int totalHoles = 0;
	    //iterates through the partitions and counts how many are free (holes)
	    for(Partition part: partList) {
	        if(part.isbFree()) {
	            totalFreeMemory += part.getLength();
	            totalHoles++;
	        }
	    }
	    //calculates the avg hole size and finds the percent of memory that is free
	    double avgHoleSize = totalHoles > 0 ? (double) totalFreeMemory / totalHoles : 0;
	    double percentFreeMemory = (double) totalFreeMemory / this.size * 100;
	    //prints the stats of the memory
	    System.out.println("\n------------------------------------------------------------------------------"
				+ "-------------------------------------------------------------------------------------------"
				+ "---------------------------------------------------------------------------------------------");
	    System.out.println("\nNumber of holes: " + totalHoles);
	    System.out.println("Average size of holes: " + avgHoleSize + " KB");
	    System.out.println("Total size of holes: " + totalFreeMemory + " KB");
	    System.out.println("Percentage of total free memory: " + percentFreeMemory + "%");
		}
      
	// sort the list of partitions in ascending order of base addresses
	private void order_partitions() {
		Collections.sort(partList, (o1, o2) -> (o1.getBase() - o2.getBase()) );
	}

	//implements the best fit memory allocation algorithm
	public int best_fit(String process, int size) {
		//looks to see if the process is in the allocated map (if it is, returns -1)
	    if(allocMap.containsKey(process)) {
	        return -1;
	    }
	    int index = 0, alloc = -1, smIndex = -1;
	    while(index < partList.size()) {
	        Partition part = partList.get(index);
	        
	        if(part.isbFree() && part.getLength()>= size) {
	            if(smIndex == -1 || part.getLength() < partList.get(smIndex).getLength()) {
	                smIndex = index;
	            }
	        }
	        index++;
	    }
	    if(smIndex != -1) {
	        Partition part = partList.get(smIndex);
	        Partition allocPart = new Partition(part.getBase(), size);
	        allocPart.setbFree(false);
	        allocPart.setProcess(process);
	        partList.add(smIndex, allocPart);
	        allocMap.put(process, allocPart);
	        part.setBase(part.getBase()+size);
	        part.setLength(part.getLength() - size);
	        if(part.getLength() == 0) {
	            partList.remove(part);
	        }
	        alloc = size;
	    }
	    return alloc;
	}

	//implements the worst fit memory allocation algorithm
	public int worst_fit(String process, int size) {
		    if(allocMap.containsKey(process)) {
		        return -1;
		    }
		    int index = 0, alloc = -1, lgIndex = -1;
		    while(index < partList.size()) {
		        Partition part = partList.get(index);
		        if(part.isbFree() && part.getLength()>= size) {
		            if(lgIndex == -1 || part.getLength() > partList.get(lgIndex).getLength()) {
		                lgIndex = index;
		            }
		        }
		        index++;
		    }
		    if(lgIndex != -1) {
		        Partition part = partList.get(lgIndex);
		        Partition allocPart = new Partition(part.getBase(), size);
		        allocPart.setbFree(false);
		        allocPart.setProcess(process);
		        partList.add(lgIndex, allocPart);
		        allocMap.put(process, allocPart);
		        part.setBase(part.getBase()+size);
		        part.setLength(part.getLength() - size);
		        if(part.getLength() == 0) {
		            partList.remove(part);
		        }
		        alloc = size;
		    }
		    return alloc;
		}

	//implements the next fit memory allocation algorithm
	public int next_fit(String process, int size) {
	    if(allocMap.containsKey(process)) {
	        return -1; //illegal request as process has already been allocated a partition
	    }
	    int index = nextFitStartIndex, alloc = -1;
	    do {
	        Partition part = partList.get(index);
	        if(part.isbFree() && part.getLength()>= size) { //found a free partition that is right size
	            Partition allocPart = new Partition(part.getBase(), size);
	            allocPart.setbFree(false);
	            allocPart.setProcess(process);
	            partList.add(index, allocPart); //insert this allocated partition at index
	            allocMap.put(process, allocPart);
	            part.setBase(part.getBase()+size);
	            part.setLength(part.getLength() - size);
	            if(part.getLength() == 0) { //if this new free memory partition has no size, remove it
	                partList.remove(part);
	            }
	            alloc = size;
	            break;
	        }
	        index = (index + 1) % partList.size(); //move to the next partition, wrap around to the start if necessary
	    } while(index != nextFitStartIndex);
	    nextFitStartIndex = index; //update the start index for the next fit search
	    return alloc;
	}

	//release the processes from the memory
	public int release(String process) {
		if(!allocMap.containsKey(process)) {
			return -1; //no such partition is allocated to process
		}
		int size = -1;
		for(Partition part : partList) {
			if(!part.isbFree() && process.equals(part.getProcess())){
				part.setbFree(true);
				part.setProcess(null);
				size = part.getLength();
				break;
			}
		}
		if(size<0) {
			return size;
		}
		else {
			merge_holes();//merge nearby memory partitions together
			return size;
		}
		
	}      
	
	//merges the adjacent holes in the memory
	private void merge_holes() {
	    order_partitions(); //order partitions based on increase base address
	    int i = 0;
	    while(i < partList.size()) {
	        Partition part = partList.get(i);
	        if(part.isbFree()) {
	            int endAddr = part.getBase() + part.getLength();
	            int j = i + 1;
	            while(j < partList.size()) {
	                Partition nextPart = partList.get(j);
	                if(nextPart.isbFree() && nextPart.getBase() == endAddr) {
	                    // Merge the next partition into the current one
	                    part.setLength(part.getLength() + nextPart.getLength());
	                    // Remove the next partition from the list
	                    partList.remove(j);
	                    //checks to see if the nextfit index is out of bounds
	                    if(nextFitStartIndex >= partList.size()) {
	                    	nextFitStartIndex = partList.size()-1;
	                    }
	                    // Update the end address
	                    endAddr = part.getBase() + part.getLength();
	                } else {
	                    // Stop merging when the next partition is not free or not adjacent
	                    break;
	                }
	            }
	        }
	        i++;
	    }
	}

	//main method to drive the program
	public static void main(String[] args) {
		//makes variables to store the info off the txt file
		int MEMORY_MAX = 0;
        int PROC_SIZE_MAX = 0;
        int NUM_PROC = 0;
        int MAX_PROC_TIME = 0;
        //connects to the file and reads off the informations and assigns it to the appropriate integer 
		try {
            File file = new File("src/info.txt");
            Scanner scanner = new Scanner(file);
           
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(" = ");

                switch (parts[0]) {
                    case "MEMORY_MAX":
                        MEMORY_MAX = Integer.parseInt(parts[1]);
                        break;
                    case "PROC_SIZE_MAX":
                        PROC_SIZE_MAX = Integer.parseInt(parts[1]);
                        break;
                    case "NUM_PROC":
                        NUM_PROC = Integer.parseInt(parts[1]);
                        break;
                    case "MAX_PROC_TIME":
                        MAX_PROC_TIME = Integer.parseInt(parts[1]);
                        break;
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
		//initializes an array list of processes
		List<Process> processes = new ArrayList<>();
		Random rand = new Random();

		//creates the allowed amount of processes and generates a random size and time within the limit
		for (int i = 0; i < NUM_PROC; i++) {
		    String name = "P" + (i + 1);
		    int size = rand.nextInt(PROC_SIZE_MAX) + 1; // random size between 1 and PROC_SIZE_MAX
		    int time = rand.nextInt(MAX_PROC_TIME) + 1; // random time between 1 and MAX_PROC_TIME
		    processes.add(new Process(name, size, time));
		}
		
		//starts the memory with the allowed memory size
		ContigousMemoryAllocator allocator = new ContigousMemoryAllocator(MEMORY_MAX);

		double stime = 0;
		double etime = 0;
		
		//Allows the user to choose which version of memory allocation they want to use
        Scanner scn = new Scanner(System.in);
        System.out.print("Which version would you like to do?\nBest Fit [1]\nWorst fit [2]\nNext fit [3]\n::");
        int mode = scn.nextInt();
       
        //user chose Best Fit
        if(mode == 1) {
        	System.out.println("You have chosen best fit.");
        	//sets start time of program
        	stime = System.currentTimeMillis();
        	//makes a queue for the waiting processes
        	Queue<Process> waitingQueue = new LinkedList<>();
        	//goes through the process and finds if they can fit in a memory hole; if not the process is sent to the waiting queue
        	for (Process process : processes) {
        	    int allocatedSize = allocator.best_fit(process.getName(), process.getSize());
        	    if (allocatedSize == -1) {
        	    	System.out.println("\n------------------------------------------------------------------------------"
        					+ "-------------------------------------------------------------------------------------------"
        					+ "---------------------------------------------------------------------------------------------");
        	        System.out.println("\nFailed to allocate memory for process " + process.getName() + ", putting it on hold.");
        	        waitingQueue.add(process);
        	    } else {
        	    	System.out.println("\n------------------------------------------------------------------------------"
        					+ "-------------------------------------------------------------------------------------------"
        					+ "---------------------------------------------------------------------------------------------");
        	        System.out.println("\nAllocated " + allocatedSize + "KB memory for process " + process.getName());
        	    }
        	}
        	
        	int tickCounter = 0;
        	//runs while both array lists have an item in them and ticks down the time for each process and releases when time hits zero
        	while (!processes.isEmpty() || !waitingQueue.isEmpty()) {
        		//creates an iterator to iterate over the collection of processes
        	    Iterator<Process> iterator = processes.iterator();
        	    while (iterator.hasNext()) {
        	        Process process = iterator.next();
        	        if (process.getTime() == 0) {
        	            // Release the process from memory if it is out of time
        	            int releasedSize = allocator.release(process.getName());
        	            if (releasedSize != -1) {
        	            	System.out.println("\n------------------------------------------------------------------------------"
        	        				+ "-------------------------------------------------------------------------------------------"
        	        				+ "---------------------------------------------------------------------------------------------");
        	                System.out.println("\nReleased " + releasedSize + "KB memory from process " + process.getName());
        	            }
        	            // Remove the process from the list
        	            iterator.remove();
        	        } else {
        	            // Decrease the process's time 
        	            process.setTime(process.getTime() - 1);
        	        }
        	    }

        	    // Try to allocate memory for the processes on hold
        	    iterator = waitingQueue.iterator();
        	    while (iterator.hasNext()) {
        	        Process process = iterator.next();
        	        int allocatedSize = allocator.best_fit(process.getName(), process.getSize());
        	        if (allocatedSize != -1) {
        	        	System.out.println("\n------------------------------------------------------------------------------"
        	    				+ "-------------------------------------------------------------------------------------------"
        	    				+ "---------------------------------------------------------------------------------------------");
        	            System.out.println("\nAllocated " + allocatedSize + "KB memory for process " + process.getName());
        	            processes.add(process);
        	            iterator.remove();
        	        }
        	    }

        	    // Increment the tick counter
        	    tickCounter++;

        	    // Print the status of the memory allocation every 1000 ticks (1 "second")
        	    if (tickCounter == 1000) {
        	        allocator.print_status(processes);
        	        allocator.printMemoryStats();
        	        tickCounter = 0;  // Reset the tick counter
        	    }
        	}
        	//posts the time it took the algorithm to run
        	etime = System.currentTimeMillis();
        	allocator.print_status(processes);
        	System.out.println("\n------------------------------------------------------------------------------"
    				+ "-------------------------------------------------------------------------------------------"
    				+ "---------------------------------------------------------------------------------------------");
        	System.out.println("\nThe time to execute best fit is " + (etime - stime) + "ms");
        	System.out.println("\n------------------------------------------------------------------------------"
    				+ "-------------------------------------------------------------------------------------------"
    				+ "---------------------------------------------------------------------------------------------");
        }
        else if(mode == 2) {
        	System.out.println("You have chosen worst fit.");
        	stime = System.currentTimeMillis();
        	Queue<Process> waitingQueue = new LinkedList<>();
        	for (Process process : processes) {
        	    int allocatedSize = allocator.worst_fit(process.getName(), process.getSize());
        	    if (allocatedSize == -1) {
        	    	System.out.println("\n------------------------------------------------------------------------------"
        					+ "-------------------------------------------------------------------------------------------"
        					+ "---------------------------------------------------------------------------------------------");
        	        System.out.println("\nFailed to allocate memory for process " + process.getName() + ", putting it on hold.");
        	        waitingQueue.add(process);
        	    } else {
        	    	System.out.println("\n------------------------------------------------------------------------------"
        					+ "-------------------------------------------------------------------------------------------"
        					+ "---------------------------------------------------------------------------------------------");
        	        System.out.println("\nAllocated " + allocatedSize + "KB memory for process " + process.getName());
        	    }
        	}
        	int tickCounter = 0;

        	while (!processes.isEmpty() || !waitingQueue.isEmpty()) {
        	    // Tick down the time for each process and release it if its time is up
        	    Iterator<Process> iterator = processes.iterator();
        	    while (iterator.hasNext()) {
        	        Process process = iterator.next();
        	        if (process.getTime() == 0) {
        	            // Release the process from memory
        	            int releasedSize = allocator.release(process.getName());
        	            if (releasedSize != -1) {
        	            	System.out.println("\n------------------------------------------------------------------------------"
        	        				+ "-------------------------------------------------------------------------------------------"
        	        				+ "---------------------------------------------------------------------------------------------");
        	                System.out.println("\nReleased " + releasedSize + "KB memory from process " + process.getName());
        	            }
        	            // Remove the process from the list
        	            iterator.remove();
        	        } else {
        	            // Decrease the process's time
        	            process.setTime(process.getTime() - 1);
        	        }
        	    }

        	    // Try to allocate memory for the processes on hold
        	    iterator = waitingQueue.iterator();
        	    while (iterator.hasNext()) {
        	        Process process = iterator.next();
        	        int allocatedSize = allocator.worst_fit(process.getName(), process.getSize());
        	        if (allocatedSize != -1) {
        	        	System.out.println("\n------------------------------------------------------------------------------"
        	    				+ "-------------------------------------------------------------------------------------------"
        	    				+ "---------------------------------------------------------------------------------------------");
        	            System.out.println("\nAllocated " + allocatedSize + "KB memory for process " + process.getName());
        	            processes.add(process);
        	            iterator.remove();
        	        }
        	    }

        	    // Increment the tick counter
        	    tickCounter++;

        	    // Print the status of the memory allocation every 1000 ticks
        	    if (tickCounter == 1000) {
        	        allocator.print_status(processes);
        	        allocator.printMemoryStats();
        	        tickCounter = 0;  // Reset the tick counter
        	    }
        	}
        	etime = System.currentTimeMillis();
        	allocator.print_status(processes);
        	System.out.println("\n------------------------------------------------------------------------------"
    				+ "-------------------------------------------------------------------------------------------"
    				+ "---------------------------------------------------------------------------------------------");
        	System.out.println("\nThe time to execute with worst fit is " + (etime-stime) + "ms");
        	System.out.println("\n------------------------------------------------------------------------------"
    				+ "-------------------------------------------------------------------------------------------"
    				+ "---------------------------------------------------------------------------------------------");
        }
        else if(mode == 3) {
        	System.out.println("You have chosen next fit.");
        	stime = System.currentTimeMillis();
        	Queue<Process> waitingQueue = new LinkedList<>();
        	for (Process process : processes) {
        	    int allocatedSize = allocator.next_fit(process.getName(), process.getSize());
        	    if (allocatedSize == -1) {
        	    	System.out.println("\n------------------------------------------------------------------------------"
        					+ "-------------------------------------------------------------------------------------------"
        					+ "---------------------------------------------------------------------------------------------");
        	        System.out.println("\nFailed to allocate memory for process " + process.getName() + ", putting it on hold.");
        	        waitingQueue.add(process);
        	    } else {
        	    	System.out.println("\n------------------------------------------------------------------------------"
        					+ "-------------------------------------------------------------------------------------------"
        					+ "---------------------------------------------------------------------------------------------");
        	        System.out.println("\nAllocated " + allocatedSize + "KB memory for process " + process.getName());
        	    }
        	}
        	int tickCounter = 0;

        	while (!processes.isEmpty() || !waitingQueue.isEmpty()) {
        	    // Tick down the time for each process and release it if its time is up
        	    Iterator<Process> iterator = processes.iterator();
        	    while (iterator.hasNext()) {
        	        Process process = iterator.next();
        	        if (process.getTime() == 0) {
        	            // Release the process from memory
        	            int releasedSize = allocator.release(process.getName());
        	            if (releasedSize != -1) {
        	            	System.out.println("\n------------------------------------------------------------------------------"
        	        				+ "-------------------------------------------------------------------------------------------"
        	        				+ "---------------------------------------------------------------------------------------------");
        	                System.out.println("\nReleased " + releasedSize + "KB memory from process " + process.getName());
        	            }
        	            // Remove the process from the list
        	            iterator.remove();
        	        } else {
        	            // Decrease the process's time
        	            process.setTime(process.getTime() - 1);
        	        }
        	    }

        	    // Try to allocate memory for the processes on hold
        	    iterator = waitingQueue.iterator();
        	    while (iterator.hasNext()) {
        	        Process process = iterator.next();
        	        int allocatedSize = allocator.next_fit(process.getName(), process.getSize());
        	        if (allocatedSize != -1) {
        	        	System.out.println("\n------------------------------------------------------------------------------"
        	    				+ "-------------------------------------------------------------------------------------------"
        	    				+ "---------------------------------------------------------------------------------------------");
        	            System.out.println("\nAllocated " + allocatedSize + "KB memory for process " + process.getName());
        	            processes.add(process);
        	            iterator.remove();
        	        }
        	    }
        	    // Increment the tick counter
        	    tickCounter++;

        	    // Print the status of the memory allocation every 1000 ticks
        	    if (tickCounter == 1000) {
        	        allocator.print_status(processes);
        	        allocator.printMemoryStats();
        	        tickCounter = 0;  // Reset the tick counter
        	    }
        	}
        	etime = System.currentTimeMillis();
        	allocator.print_status(processes);
        	System.out.println("\n------------------------------------------------------------------------------"
    				+ "-------------------------------------------------------------------------------------------"
    				+ "---------------------------------------------------------------------------------------------");
        	System.out.println("\nThe time to execute with next fit is " + (etime - stime) + "ms");
        	System.out.println("\n------------------------------------------------------------------------------"
    				+ "-------------------------------------------------------------------------------------------"
    				+ "---------------------------------------------------------------------------------------------");
        }
        //user did not input a valid option and must restart
        else {
        	System.out.println("Please restart the program and choose a valid option.");
        }
	}
}
