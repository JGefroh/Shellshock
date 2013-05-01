package systems;

import infopacks.IInfoPack;
import infopacks.IInfoPackFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import entities.IEntity;

/**
 * The core class is the heart of the game. 
 * It keeps track of all Entities, InfoPacks, and Systems.
 * It monitors Entities for changes in their composition, and automatically
 * updates their associated InfoPacks.
 * @author Joseph Gefroh
 */
public class Core
{
	//private ArrayList<IInfoPack> infoPacks;
	private HashMap<IEntity, ArrayList<IInfoPack>> infoPacks;
	private ArrayList<IEntity> entities;
	private ArrayList<ISystem> systems;	
	private ArrayList<IInfoPackFactory> packFactories;
	private boolean debug = true;
	private final static Logger LOGGER 
		= Logger.getLogger(Core.class.getName());
	
	private void initLogger()
	{
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(Level.ALL);
		LOGGER.addHandler(ch);
		LOGGER.setLevel(Level.ALL);
	}
	/**
	 * Create a new Core object.
	 */
	public Core()
	{
		initLogger();
		infoPacks = new HashMap<IEntity, ArrayList<IInfoPack>>();
		entities = new ArrayList<IEntity>();
		systems = new ArrayList<ISystem>();
		packFactories = new ArrayList<IInfoPackFactory>();
	}
	
	/**
	 * Begin tracking an entity.
	 * @param entity	the Entity to track.
	 */
	public void addEntity(final IEntity entity)
	{
		if(entity!=null&&entities.contains(entity)==false)
		{//if the entity exists and it is not already being tracked...
			LOGGER.log(Level.FINER, "Adding new entity: " + entity);
			entities.add(entity);
			generateInfoPacks(entity);
		}
	}
	
	/**
	 * Start tracking and using an info pack associated with a given entity.
	 * @param entity		the entity that "owns" the InfoPack
	 * @param infoPack		the InfoPack to track
	 */
	public void addInfoPack(final IEntity entity, final IInfoPack infoPack)
	{	
		if(infoPack!=null&&infoPacks.containsValue(infoPack)==false)
		{//If the infopack is not already being tracked
			LOGGER.log(Level.FINER, "Adding " + infoPack + " to: " + entity);
			ArrayList<IInfoPack> entityPacks = infoPacks.get(entity);
			if(entityPacks!=null)
			{//If an arraylist already exists for the info pack
				entityPacks.add(infoPack);	//Add the info pack to the arraylist
			}
			else
			{//If not...
				//Construct an array list.
				entityPacks = new ArrayList<IInfoPack>();
				entityPacks.add(infoPack);
				infoPacks.put(entity, entityPacks);
			}
		}
	}

	/**
	 * Begin tracking and using a system.
	 * @param system	the System to track and use
	 * @param priority	the priority of the system
	 * 					</br> Systems execute based on their priority level.
	 * 					</br> 0 is the highest priority, with increasing numbers 
	 * 					being of lower priority.
	 * 					</br> A priority of -1 is used to indicate the lowest 
	 * 					current priority and should be used when the order
	 * 					of execution does not matter for the system.
	 * 					
	 */
	public void addSystem(final ISystem system, final int priority)
	{
		if(system!=null&&systems.contains(system)==false)
		{
			LOGGER.log(Level.FINER, "Adding system: " + system 
						+ " with priority: " + priority);
			system.start();
			if(priority>=0)
			{
				systems.add(priority, system);
			}
			else
			{
				systems.add(system);
			}
		}
	}

	/**
	 * Stop tracking an entity.
	 * @param entity	the Entity to stop tracking.
	 */
	public void removeEntity(final IEntity entity)
	{
		LOGGER.log(Level.FINER, "Removing entity: " + entity);
		if(entity!=null)
		{
			entities.remove(entity);
			infoPacks.remove(entity);
		}
	}
	
	/**
	 * Stop using an InfoPack.
	 * @param infoPack	the InfoPack to stop using.
	 */
	public void removeInfoPack(final IInfoPack infoPack)
	{
		if(infoPack!=null)
		{
			ArrayList<IInfoPack> entityPacks = 
					infoPacks.get(infoPack.getParent());
			if(entityPacks!=null)
			{
				LOGGER.log(Level.FINER, "Removing infoPack: " + infoPack);
				entityPacks.remove(infoPack);
			}
		}
	}

	/**
	 * Stop using a system.
	 * @param system	the System to stop using.
	 */
	public void removeSystem(final ISystem system)
	{
		if(system!=null)
		{
			LOGGER.log(Level.FINER, "Removing system: " + system);
			system.stop();
			systems.remove(system);
		}
	}
	
	/**
	 * Retrieve all tracked InfoPacks of a specific type.
	 * @param	t	the Class type of info pack to return
	 * @return		all tracked InfoPacks of the given type.
	 */
	public <T extends IInfoPack> ArrayList<T> getInfoPacksOfType(Class<T> t)
	{
		ArrayList<T> packs = new ArrayList<T>();
		Set<IEntity> entitySet = infoPacks.keySet();

		for(IEntity each:entitySet)
		{//For each entity tracked...
			//...get the packs the entity "owns"
			ArrayList<IInfoPack> entityPacks = infoPacks.get(each);
			
			for(IInfoPack entityPack:entityPacks)
			{//For each pack the entity owns....
				if(entityPack.getClass()==t)
				{//If it is an instance of the desired type, grab it.
					packs.add((T)entityPack);
				}
			}
		}
		return packs;
	}
	
	/**
	 * Get a system of a specific type
	 * @param t	the Class type of system
	 * @return	the System, if found. Null otherwise.
	 */
	public <T extends ISystem> T getSystem(Class<T> t)
	{
		for(ISystem each:systems)
		{
			if(each.getClass()==t)
			{
				return (T)each;
			}
		}
		return null;
	}
	
	/**
	 * Ensure all entities have updated InfoPacks, and then execute the system.
	 */
	public void work()
	{
		for(IEntity each:entities)
		{
			if(each.hasChanged())
			{
				generateInfoPacks(each);
			}
		}
		
		for(ISystem system:systems)
		{
			system.work();
		}
	}
	
	/**
	 * Begin using an InfoPack factory to generate InfoPacks of a specific type.
	 * @param factory	the Factory to begin using
	 */
	public void addFactory(final IInfoPackFactory factory)
	{
		if(factory!=null&&packFactories.contains(factory)==false)
		{			
			LOGGER.log(Level.FINER, "Adding factory: " + factory);
			packFactories.add(factory);
		}
	}
	
	/**
	 * Generate InfoPacks with known factories for the given entity.
	 * @param entity	the Entity to generate InfoPacks for
	 */
	public void generateInfoPacks(final IEntity entity)
	{
		if(entity!=null)
		{
			LOGGER.log(Level.FINER, "Generating infoPacks for: " + entity);
			infoPacks.remove(entity);
			for(IInfoPackFactory each:packFactories)
			{
				IInfoPack pack = each.generate(entity);
				if(pack!=null&&pack.updateReferences())
				{					
					addInfoPack(entity, pack);
				}
			}
			entity.setChanged(false);
		}
	}
	
	/**
	 * Get a specific InfoPack belonging to a specific entity.
	 * @param entity	the Entity that owns the InfoPack
	 * @param type		the type of InfoPack to get
	 * @return	an InfoPack of the type requested, null if not found.
	 */
	public <T extends IInfoPack>T getInfoPackFrom(final IEntity entity, final Class<T> type)
	{
		ArrayList<IInfoPack> packs = infoPacks.get(entity);
		if(packs!=null)
		{
			for(IInfoPack each:packs)
			{
				if(each.getClass()==type)
				{
					return (T)each;
				}
			}
		}
		return null;
	}
}
