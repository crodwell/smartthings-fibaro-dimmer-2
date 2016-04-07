/**
 *  Device Type Definition File
 *
 *  Device Type:		Fibaro Dimmer 2
 *  File Name:			fibaro-dimmer-2.groovy
 *	Initial Release:	2016-04-07
 *	Author:				Chris Rodwell
 *
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	definition (name: "Fibaro Dimmer 2", namespace: "shazbot", author: "crodwell") {
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
                        
        command		"resetParams2Defaults"
        command		"listCurrentParams"
        command     "configure2WayAndOffSwitch"
        command		"updateZwaveParam"

        fingerprint deviceId: "0x1001", inClusters: "0x5E, 0x20, 0x86, 0x72, 0x26, 0x5A, 0x59, 0x85, 0x73, 0x98, 0x7A, 0x56, 0x70, 0x31, 0x32, 0x8E, 0x60, 0x75, 0x71, 0x27, 0x22, 0xEF, 0x2B"
	}

	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"
		status "09%": "command: 2003, payload: 09"
		status "10%": "command: 2003, payload: 0A"
		status "33%": "command: 2003, payload: 21"
		status "66%": "command: 2003, payload: 42"
		status "99%": "command: 2003, payload: 63"

		// reply messages
		reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
		reply "200100,delay 5000,2602": "command: 2603, payload: 00"
		reply "200119,delay 5000,2602": "command: 2603, payload: 19"
		reply "200132,delay 5000,2602": "command: 2603, payload: 32"
		reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
		reply "200163,delay 5000,2602": "command: 2603, payload: 63"
	}

	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
			state "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
			state "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
			state "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 3, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}

		main(["switch"])
		details(["switch", "refresh", "levelSliderControl"])
	}
}

def parse(String description) {
	def item1 = [
		canBeCurrentState: false,
		linkText: getLinkText(device),
		isStateChange: false,
		displayed: false,
		descriptionText: description,
		value:  description
	]
	def result
	def cmd = zwave.parse(description, [0x26: 1, 0x70: 2, 072: 2])
    //log.debug "cmd: ${cmd}"
    
    if (cmd) {
        result = createEvent(cmd, item1)
	}
	else {
		item1.displayed = displayed(description, item1.isStateChange)
		result = [item1]
	}
    
    if(result?.descriptionText)
		log.debug "Parse returned ${result?.descriptionText}"
        
	result

}

def createEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, Map item1) {
	def result = doCreateEvent(cmd, item1)
	for (int i = 0; i < result.size(); i++) {
		result[i].type = "physical"
	}
	result
}

def createEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, Map item1) {
	def result = doCreateEvent(cmd, item1)
	for (int i = 0; i < result.size(); i++) {
		result[i].type = "physical"
	}
	result
}

def createEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStartLevelChange cmd, Map item1) {
	[]
}

def createEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd, Map item1) {
	[response(zwave.basicV1.basicGet())]
}

def createEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelSet cmd, Map item1) {
	def result = doCreateEvent(cmd, item1)
	for (int i = 0; i < result.size(); i++) {
		result[i].type = "physical"
	}
	result
}

def createEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd, Map item1) {
	def result = doCreateEvent(cmd, item1)
	result[0].descriptionText = "${item1.linkText} is ${item1.value}"
	result[0].handlerName = cmd.value ? "statusOn" : "statusOff"
	for (int i = 0; i < result.size(); i++) {
		result[i].type = "digital"
	}
	result
}

def doCreateEvent(physicalgraph.zwave.Command cmd, Map item1) {
	def result = [item1]

	item1.name = "switch"
	item1.value = cmd.value ? "on" : "off"
	item1.handlerName = item1.value
	item1.descriptionText = "${item1.linkText} was turned ${item1.value}"
	item1.canBeCurrentState = true
	item1.isStateChange = isStateChange(device, item1.name, item1.value)
	item1.displayed = item1.isStateChange

	if (cmd.value >= 5) {
		def item2 = new LinkedHashMap(item1)
		item2.name = "level"
		item2.value = cmd.value as String
		item2.unit = "%"
		item2.descriptionText = "${item1.linkText} dimmed ${item2.value} %"
		item2.canBeCurrentState = true
		item2.isStateChange = isStateChange(device, item2.name, item2.value)
		item2.displayed = false
		result << item2
	}
	result
}

def createEvent(physicalgraph.zwave.Command cmd,  Map map) {
	// Handles any Z-Wave commands we aren't interested in
	log.debug "UNHANDLED COMMAND $cmd"
}

def on() {
	log.info "on"
	delayBetween([zwave.basicV1.basicSet(value: 0xFF).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 5000)
}

def off() {
	delayBetween ([zwave.basicV1.basicSet(value: 0x00).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 5000)
}

def setLevel(value) {
    def level = Math.min(value as Integer, 99)
	delayBetween ([zwave.basicV1.basicSet(value: level).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 5000)
}

def setLevel(value, duration) {
    def level = Math.min(value as Integer, 99)
	def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
	zwave.switchMultilevelV2.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration).format()
}

def poll() {
	zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def refresh() {
	zwave.switchMultilevelV1.switchMultilevelGet().format()
}

 /**
 * Configures the device to settings needed by SmarthThings at device discovery time. Assumes
 * device is already at default parameter settings.
 *
 * @param none
 *
 * @return none
 */
def configure() {
	log.debug "Configuring Device..."
	def cmds = []

	// send associate to group 3 to get sensor data reported only to hub
	cmds << zwave.associationV2.associationSet(groupingIdentifier:3, nodeId:[zwaveHubNodeId]).format()


	delayBetween(cmds, 500)
}


def createEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd, Map item1) {

	log.debug "${device.displayName} parameter '${cmd.parameterNumber}' with a byte size of '${cmd.size}' is set to '${cmd.configurationValue}'"

}

 /**
 * This method will allow the user to update device parameters (behavior) from an app.
 * the user can write his/her own app to envoke this method. 
 * No type or value checking is done to compare to what device capability or reaction.
 * It is up to user to read OEM documentation prio to envoking this method.
 * 
 * <p>THIS IS AN ADVANCED OPERATION. USE AT YOUR OWN RISK! READ OEM DOCUMENTATION!
 *
 * @param List[paramNumber:80,value:10,size:1]
 *
 *
 * @return none
 */
def updateZwaveParam(params) {
	if ( params ) {
        def pNumber = params.paramNumber
        def pSize	= params.size
        def pValue	= [params.value]
        log.debug "Make sure device is awake and in recieve mode"
        log.debug "Updating ${device.displayName} parameter number '${pNumber}' with value '${pValue}' with size of '${pSize}'"

		def cmds = []
        cmds << zwave.configurationV1.configurationSet(configurationValue: pValue, parameterNumber: pNumber, size: pSize).format()
        cmds << zwave.configurationV1.configurationGet(parameterNumber: pNumber).format()
        delayBetween(cmds, 1000)        
    }
}

 /**
 * Sets all of available Fibaro parameters back to the device defaults
 *
 * <p>THIS IS AN ADVANCED OPERATION. USE AT YOUR OWN RISK! READ OEM DOCUMENTATION!
 *
 * @param none
 *
 * @return none
 */
def resetParams2Defaults() {
	log.debug "Resetting ${device.displayName} parameters to Fibaro defaults"
	def cmds = []
    // GROUP 0 - The Dimmer 2 behavior - Basic functionalities 
	cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 1,  size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [99],   parameterNumber: 2,  size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 3,  size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 4,  size: 2).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 5,  size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 6,  size: 2).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 7,  size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [5],    parameterNumber: 8,  size: 2).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 9,  size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 10, size: 2).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [255],  parameterNumber: 11, size: 2).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 13, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 14, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [30],   parameterNumber: 15, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [5],    parameterNumber: 16, size: 2).format()
    // GROUP 20 - Dimmer 2 operation - Switches
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 20, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 21, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 22, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 23, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 24, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 25, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 26, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [15],   parameterNumber: 27, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 28, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 29, size: 1).format()
    // GROUP 30 - Dimmer 2 operation - Advanced functionality
    cmds << zwave.configurationV1.configurationSet(configurationValue: [2],    parameterNumber: 30, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [2],    parameterNumber: 32, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 34, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 35, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 37, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [250],  parameterNumber: 39, size: 2).format()
    // GROUP 40 - Dimmer 2 operation - Alarms
    cmds << zwave.configurationV1.configurationSet(configurationValue: [3],    parameterNumber: 40, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [2],    parameterNumber: 41, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [3],    parameterNumber: 42, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 43, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [600],  parameterNumber: 44, size: 2).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 45, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 46, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 47, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 48, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 49, size: 1).format()
    // GROUP 50 - Active power and energy reports
    cmds << zwave.configurationV1.configurationSet(configurationValue: [10],   parameterNumber: 50, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [3600], parameterNumber: 52, size: 2).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [10],   parameterNumber: 53, size: 2).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 54, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 58, size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [0],    parameterNumber: 59, size: 2).format()
    
    
    
    delayBetween(cmds, 500)
}

def configure2WayAndOffSwitch() {
	log.debug "Configuring ${device.displayName} parameters to be a 2 Way and Off roller blind switch"
	def cmds = []
	cmds << zwave.configurationV1.configurationSet(configurationValue: [2],    parameterNumber: 20,  size: 1).format()
    cmds << zwave.configurationV1.configurationSet(configurationValue: [1],    parameterNumber: 29, size: 1).format()

    delayBetween(cmds, 500)
}

 /**
 * Lists all of available Fibaro parameters and thier current settings out to the 
 * logging window in the IDE. This will be called from the "Fibaro Tweaker" or 
 * user's own app.
 *
 * <p>THIS IS AN ADVANCED OPERATION. USE AT YOUR OWN RISK! READ OEM DOCUMENTATION!
 *
 * @param none
 *
 * @return none
 */
def listCurrentParams() {
	log.debug "Listing of current parameter settings of ${device.displayName}"
    def cmds = []
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 1).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 2).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 4).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 5).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 6).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 7).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 8).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 9).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 10).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 11).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 12).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 13).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 14).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 15).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 16).format()
    
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 20).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 21).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 22).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 23).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 24).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 25).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 26).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 27).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 28).format()
    cmds << zwave.configurationV1.configurationGet(parameterNumber: 29).format()
    
    
	delayBetween(cmds, 500)
}
